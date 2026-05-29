package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.room.dto.request.RoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.response.RoomResponse;
import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;
import com.ssafy.meari.domain.room.entity.MemberRoom;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import com.ssafy.meari.domain.room.repository.MemberRoomRepository;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final MemberRoomRepository memberRoomRepository;
    private final MemberRepository memberRepository;
    private final ThemeRepository themeRepository;
    private final ContentRepository contentRepository;
    private final RoomSessionService roomSessionService;
    private final RoomBroadcastService roomBroadcastService;

    /**
     * 방 생성
     */
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request, Long memberId) {
        log.info("방 생성 시작: memberId={}, title={}", memberId, request.getTitle());

        Member owner = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        Theme theme = themeRepository.findById(request.getThemeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_THEME));

        Room room = createRoomWithOwner(owner, theme,
                request.getTitle(), request.getMaxPeople(), request.getPassword());

        return RoomResponse.from(room, 1);
    }

    /**
     * 빠른 방 생성 (테마 배너 클릭)
     * 테마 ID만 받아 랜덤 콘텐츠를 선택하고 자동으로 방을 생성합니다.
     */
    @Transactional
    public RoomResponse createQuickRoom(Long themeId, Long memberId) {
        log.info("빠른 방 생성 시작: memberId={}, themeId={}", memberId, themeId);

        Member owner = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_THEME));

        // 해당 테마의 콘텐츠 목록 조회 및 랜덤 선택
        List<Content> contents = contentRepository.findByTheme_ThemeId(themeId);
        if (contents.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_CONTENT_IN_THEME);
        }
        Content selectedContent = contents.get(ThreadLocalRandom.current().nextInt(contents.size()));
        String title = theme.getName() + " 테마 같이 공부해요~";

        Room room = createRoomWithOwner(owner, theme,
                title, selectedContent.getMaxPeople(), null);

        // Redis에 콘텐츠 설정 (방 생성 즉시)
        roomSessionService.setContent(room.getRoomId(), selectedContent.getContentId());
        log.info("빠른 방 생성 완료: roomId={}, contentId={}", room.getRoomId(), selectedContent.getContentId());

        return RoomResponse.from(room, 1);
    }

    /**
     * 방 + 방장 입장(MemberRoom) + Redis 세션 등록을 한 단위로 처리.
     * createRoom / createQuickRoom 공통 본문 추출.
     */
    private Room createRoomWithOwner(Member owner, Theme theme, String title,
                                     Integer maxPeople, String password) {
        Room room = Room.builder()
                .owner(owner)
                .theme(theme)
                .title(title)
                .maxPeople(maxPeople)
                .password(password)
                .build();
        room = roomRepository.save(room);

        memberRoomRepository.save(MemberRoom.builder().room(room).member(owner).build());

        roomSessionService.addMember(room.getRoomId(), owner.getMemberId());
        roomSessionService.setMemberRoom(owner.getMemberId(), room.getRoomId());

        return room;
    }

    /**
     * 방 입장
     * 비정상 종료는 SessionDisconnectEvent에서 즉시 처리되므로,
     * 여기서는 단순히 DB 중복만 체크 (Redis는 이미 정리됨)
     */
    @Transactional
    public void enterRoom(Long roomId, RoomEnterRequest request, Long memberId) {
        log.info("방 입장 시도: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 입장 가능 상태인지 확인
        if (!room.isJoinable()) {
            throw new BusinessException(ErrorCode.ROOM_NOT_JOINABLE);
        }

        // 비밀번호 확인
        if (!room.isPasswordMatch(request.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_ROOM_PASSWORD);
        }

        // Redis에 이미 있으면 중복 입장 (현재 접속 중)
        if (roomSessionService.isMember(roomId, memberId)) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_JOINED);
        }

        // DB에 잔존 데이터가 있으면 삭제 (비정상 종료 후 재입장)
        memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(roomId, memberId)
                .ifPresent(existingMemberRoom -> {
                    memberRoomRepository.delete(existingMemberRoom);
                    log.info("비정상 종료 잔존 데이터 삭제: roomId={}, memberId={}", roomId, memberId);
                });

        // 정원 확인
        long currentCount = memberRoomRepository.countByRoom_RoomId(roomId);
        if (currentCount >= room.getMaxPeople()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        // MemberRoom 저장
        MemberRoom memberRoom = MemberRoom.builder()
                .room(room)
                .member(member)
                .build();
        memberRoomRepository.save(memberRoom);

        // Redis에 참여자 추가 및 멤버→방 매핑
        roomSessionService.addMember(roomId, memberId);
        roomSessionService.setMemberRoom(memberId, roomId);

        // 입장 알림 브로드캐스트
        RoomStateMessage message = RoomStateMessage.memberJoin(memberId, member.getNickname());
        roomBroadcastService.broadcastState(roomId, message);

        log.info("방 입장 완료: roomId={}, memberId={}", roomId, memberId);
    }

    /**
     * 방 퇴장 (정상 퇴장)
     */
    @Transactional
    public void leaveRoom(Long roomId, Long memberId) {
        log.info("방 퇴장 시도: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 참여 중인지 확인 (WebSocket disconnect로 이미 처리된 경우 정상 종료)
        MemberRoom memberRoom = memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(roomId, memberId)
                .orElse(null);

        if (memberRoom == null) {
            log.info("이미 퇴장 처리됨 (WebSocket disconnect): roomId={}, memberId={}", roomId, memberId);
            return;
        }

        // MemberRoom 삭제
        memberRoomRepository.delete(memberRoom);

        // Redis에서 참여자 제거 (준비 상태, 역할도 함께 제거됨)
        roomSessionService.removeMember(roomId, memberId);
        roomSessionService.clearMemberRoom(memberId);

        // 방장이 나간 경우 처리 및 새 방장 ID 반환
        Long newOwnerId = null;
        if (room.getOwner().getMemberId().equals(memberId)) {
            newOwnerId = handleOwnerLeave(room);
        }

        // 퇴장 알림 브로드캐스트
        RoomStateMessage message = RoomStateMessage.memberLeave(memberId, newOwnerId);
        roomBroadcastService.broadcastState(roomId, message);

        // 마지막 사람이 나간 경우 방 종료
        long remainingCount = memberRoomRepository.countByRoom_RoomId(roomId);
        closeRoomIfEmpty(remainingCount, roomId, room);

        log.info("방 퇴장 완료: roomId={}, memberId={}", roomId, memberId);
    }

    /**
     * 멤버 강퇴 (방장 전용, WAITING 상태에서만)
     */
    @Transactional
    public void kickMember(Long roomId, Long targetMemberId, Long requestMemberId) {
        log.info("멤버 강퇴 요청: roomId={}, targetMemberId={}, requestMemberId={}", roomId, targetMemberId, requestMemberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 권한·상태 검증 (도메인 메서드)
        room.checkWaitingOrThrow();
        room.checkOwnerOrThrow(requestMemberId);

        // 자기 자신 강퇴 방지
        if (requestMemberId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_SELF);
        }

        // 대상 멤버가 방에 참여 중인지 확인
        MemberRoom memberRoom = memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(roomId, targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER_ROOM));

        // MemberRoom DB 삭제
        memberRoomRepository.delete(memberRoom);

        // Redis 정리 (멤버/준비상태/역할 제거 + 멤버→방 매핑 제거 + Grace Period 마킹 제거)
        roomSessionService.removeMember(roomId, targetMemberId);
        roomSessionService.clearMemberRoom(targetMemberId);
        roomSessionService.clearDisconnected(roomId, targetMemberId);

        // 강퇴 알림 브로드캐스트
        RoomStateMessage message = RoomStateMessage.memberKicked(targetMemberId);
        roomBroadcastService.broadcastState(roomId, message);

        log.info("멤버 강퇴 완료: roomId={}, targetMemberId={}", roomId, targetMemberId);
    }

    /**
     * 비정상 종료 처리 (WebSocket 연결 끊김)
     * SessionDisconnectEvent에서 호출됨
     */
    @Transactional
    public void handleAbnormalDisconnect(Long roomId, Long memberId) {
        log.info("비정상 종료 처리 시작: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            log.warn("비정상 종료 처리: 방이 존재하지 않음, roomId={}", roomId);
            return;
        }

        MemberRoom memberRoom = memberRoomRepository
                .findByRoom_RoomIdAndMember_MemberId(roomId, memberId)
                .orElse(null);

        if (memberRoom == null) {
            log.warn("비정상 종료 처리: MemberRoom이 존재하지 않음, roomId={}, memberId={}", roomId, memberId);
            return;
        }

        // DB에서 삭제
        memberRoomRepository.delete(memberRoom);
        log.debug("비정상 종료: MemberRoom 삭제 완료, roomId={}, memberId={}", roomId, memberId);

        // 방장이었으면 위임
        if (room.getOwner().getMemberId().equals(memberId)) {
            Long newOwnerId = handleOwnerLeave(room);
            log.info("비정상 종료: 방장 위임 완료, roomId={}, newOwnerId={}", roomId, newOwnerId);
        }

        // 남은 인원 확인
        long remainingCount = memberRoomRepository.countByRoom_RoomId(roomId);
        closeRoomIfEmpty(remainingCount, roomId, room);

        log.info("비정상 종료 처리 완료: roomId={}, memberId={}, 남은 인원={}", roomId, memberId, remainingCount);
    }

    /**
     * 방장 퇴장 시 처리 (위임)
     * @return 새 방장 ID (위임된 경우), 없으면 null
     */
    private Long handleOwnerLeave(Room room) {
        // 가장 먼저 입장한 사람에게 방장 위임
        return memberRoomRepository.findFirstByRoomIdOrderByCreatedAtAsc(room.getRoomId())
                .map(mr -> {
                    room.updateOwner(mr.getMember());
                    Long newOwnerId = mr.getMember().getMemberId();
                    log.info("방장 위임: roomId={}, newOwnerId={}", room.getRoomId(), newOwnerId);
                    return newOwnerId;
                })
                .orElse(null);
    }

    /**
     * 남은 인원이 없으면 방을 COMPLETED로 닫는다.
     * (leaveRoom / handleAbnormalDisconnect 공통 처리)
     */
    private void closeRoomIfEmpty(long remainingCount, Long roomId, Room room) {
        if (remainingCount == 0) {
            room.updateStatus(RoomStatus.COMPLETED);
            roomSessionService.clearRoomSession(roomId);
            log.info("방 종료 (마지막 참여자 퇴장): roomId={}", roomId);
        }
    }

}
