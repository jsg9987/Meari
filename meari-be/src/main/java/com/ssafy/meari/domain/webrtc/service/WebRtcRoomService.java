package com.ssafy.meari.domain.webrtc.service;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.room.entity.MemberRoom;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import com.ssafy.meari.domain.room.repository.MemberRoomRepository;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.domain.room.service.RoomSessionService;
import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;
import com.ssafy.meari.domain.webrtc.dto.response.OpenViduConnectionResponse;
import com.ssafy.meari.domain.webrtc.dto.response.RoomEnterWebRtcResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebRtcRoomService {

    private final RoomRepository roomRepository;
    private final MemberRoomRepository memberRoomRepository;
    private final MemberRepository memberRepository;
    private final RoomSessionService roomSessionService;
    private final OpenViduService openViduService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 방 입장 + WebRTC 토큰 발급
     */
    @Transactional
    public RoomEnterWebRtcResponse enterRoomWithWebRtc(Long roomId, String password, Long memberId, String nickname) {
        log.debug("방 입장 (WebRTC) 시도: roomId={}, memberId={}", roomId, memberId);

        // 방 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 입장 가능 상태인지 확인 (WAITING 상태만 입장 가능)
        if (!room.isJoinable()) {
            if (room.getStatus() == RoomStatus.IN_PROGRESS) {
                throw new BusinessException(ErrorCode.ROOM_ALREADY_STARTED);
            } else if (room.getStatus() == RoomStatus.COMPLETED) {
                throw new BusinessException(ErrorCode.ROOM_ALREADY_CLOSED);
            }
            throw new BusinessException(ErrorCode.ROOM_NOT_JOINABLE);
        }

        // 비밀번호 확인 (비밀방인 경우)
        if (!room.isPasswordMatch(password)) {
            throw new BusinessException(ErrorCode.INVALID_ROOM_PASSWORD);
        }

        // 이미 참여 중인지 확인
        boolean alreadyJoined = memberRoomRepository.existsByRoom_RoomIdAndMember_MemberId(roomId, memberId);

        // 정원 확인 (새로운 입장인 경우에만)
        long currentCount = memberRoomRepository.countByRoom_RoomId(roomId);
        if (!alreadyJoined && currentCount >= room.getMaxPeople()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        // 닉네임이 없으면 회원 정보에서 가져옴
        String finalNickname = nickname != null ? nickname : member.getNickname();

        // 새로운 입장인 경우에만 MemberRoom 저장
        if (!alreadyJoined) {
            MemberRoom memberRoom = MemberRoom.builder()
                    .room(room)
                    .member(member)
                    .build();
            memberRoomRepository.save(memberRoom);

            // Redis에 참여자 추가
            roomSessionService.addMember(roomId, memberId);

            // 입장 알림 브로드캐스트
            RoomStateMessage message = RoomStateMessage.memberJoin(memberId, finalNickname);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

            log.debug("방 입장 완료 (새 참여자): roomId={}, memberId={}", roomId, memberId);
        } else {
            log.debug("방 재입장 (기존 참여자): roomId={}, memberId={}", roomId, memberId);
        }

        // OpenVidu 연결 토큰 생성
        OpenViduConnectionResponse connectionResponse = openViduService.createConnectionForRoom(
                roomId, memberId, finalNickname);

        // 현재 참여자 수 조회
        int currentPeople = (int) memberRoomRepository.countByRoom_RoomId(roomId);

        return RoomEnterWebRtcResponse.of(
                roomId,
                connectionResponse.getSessionId(),
                connectionResponse.getToken(),
                connectionResponse.getConnectionId(),
                currentPeople,
                room.getMaxPeople()
        );
    }

    /**
     * 방 퇴장 (WebRTC)
     */
    @Transactional
    public void leaveRoomWithWebRtc(Long roomId, Long memberId) {
        log.debug("방 퇴장 (WebRTC) 시도: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 참여 중인지 확인
        MemberRoom memberRoom = memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER_ROOM));

        // MemberRoom 삭제
        memberRoomRepository.delete(memberRoom);

        // Redis에서 참여자 제거 (준비 상태, 역할도 함께 제거됨)
        roomSessionService.removeMember(roomId, memberId);

        // 방장이 나간 경우 처리 및 새 방장 ID 반환
        Long newOwnerId = null;
        if (room.getOwner().getMemberId().equals(memberId)) {
            newOwnerId = handleOwnerLeave(room);
        }

        // 퇴장 알림 브로드캐스트
        RoomStateMessage message = RoomStateMessage.memberLeave(memberId, newOwnerId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        // 마지막 사람이 나간 경우 방 종료
        long remainingCount = memberRoomRepository.countByRoom_RoomId(roomId);
        if (remainingCount == 0) {
            room.updateStatus(RoomStatus.COMPLETED);
            roomSessionService.clearRoomSession(roomId);

            // OpenVidu 세션도 종료 시도 (실패해도 무시)
            try {
                openViduService.closeSession("room_" + roomId);
            } catch (Exception e) {
                log.warn("OpenVidu 세션 종료 실패 (무시): sessionId=room_{}", roomId);
            }

            log.debug("방 종료 (마지막 참여자 퇴장): roomId={}", roomId);
        }

        log.debug("방 퇴장 완료: roomId={}, memberId={}", roomId, memberId);
    }

    /**
     * 방장 퇴장 시 처리 (위임)
     * @return 새 방장 ID (위임된 경우), 없으면 null
     */
    private Long handleOwnerLeave(Room room) {
        return memberRoomRepository.findFirstByRoomIdOrderByCreatedAtAsc(room.getRoomId())
                .map(mr -> {
                    room.updateOwner(mr.getMember());
                    Long newOwnerId = mr.getMember().getMemberId();
                    log.debug("방장 위임: roomId={}, newOwnerId={}", room.getRoomId(), newOwnerId);
                    return newOwnerId;
                })
                .orElse(null);
    }
}
