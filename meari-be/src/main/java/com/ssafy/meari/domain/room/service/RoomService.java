package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.room.dto.request.RoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.response.RoomDetailResponse;
import com.ssafy.meari.domain.room.dto.response.RoomListResponse;
import com.ssafy.meari.domain.room.dto.response.RoomMemberResponse;
import com.ssafy.meari.domain.room.dto.response.RoomResponse;
import com.ssafy.meari.global.common.CursorPageResponse;
import com.ssafy.meari.domain.room.entity.MemberRoom;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import com.ssafy.meari.domain.room.repository.MemberRoomRepository;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final MemberRoomRepository memberRoomRepository;
    private final MemberRepository memberRepository;
    private final ThemeRepository themeRepository;
    private final RoomSessionService roomSessionService;

    /**
     * 방 생성
     */
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request, Long memberId) {
        log.info("방 생성 시작: memberId={}, title={}", memberId, request.getTitle());

        // 회원 조회
        Member owner = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        // 테마 조회
        Theme theme = themeRepository.findById(request.getThemeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_THEME));

        // 방 생성
        Room room = Room.builder()
                .owner(owner)
                .theme(theme)
                .title(request.getTitle())
                .maxPeople(request.getMaxPeople())
                .password(request.getPassword())
                .build();

        roomRepository.save(room);
        log.info("방 생성 완료: roomId={}", room.getRoomId());

        // 방장 자동 입장 (MemberRoom)
        MemberRoom memberRoom = MemberRoom.builder()
                .room(room)
                .member(owner)
                .build();
        memberRoomRepository.save(memberRoom);

        // Redis에 참여자 추가
        roomSessionService.addMember(room.getRoomId(), memberId);

        return RoomResponse.from(room, 1);
    }

    /**
     * 방 목록 조회 (커서 기반 페이징)
     */
    public CursorPageResponse<RoomListResponse> getRoomList(Long themeId, Long cursor, int size) {
        log.info("방 목록 조회: themeId={}, cursor={}, size={}", themeId, cursor, size);

        // size + 1개 조회해서 다음 페이지 존재 여부 확인
        List<Room> rooms;
        if (themeId != null) {
            rooms = roomRepository.findRoomsWithCursor(
                    themeId, cursor, RoomStatus.COMPLETED, PageRequest.of(0, size + 1));
        } else {
            rooms = roomRepository.findAllRoomsWithCursor(
                    cursor, RoomStatus.COMPLETED, PageRequest.of(0, size + 1));
        }

        boolean hasNext = rooms.size() > size;
        if (hasNext) {
            rooms = rooms.subList(0, size);
        }

        // 각 방의 현재 인원 수 조회
        List<RoomListResponse> contents = rooms.stream()
                .map(room -> {
                    int currentPeople = (int) memberRoomRepository.countByRoom_RoomId(room.getRoomId());
                    return RoomListResponse.from(room, currentPeople);
                })
                .collect(Collectors.toList());

        Long nextCursor = hasNext && !rooms.isEmpty()
                ? rooms.get(rooms.size() - 1).getRoomId()
                : null;

        return CursorPageResponse.of(contents, nextCursor, hasNext);
    }

    /**
     * 방 상세 조회
     */
    public RoomDetailResponse getRoomDetail(Long roomId) {
        log.info("방 상세 조회: roomId={}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 참여자 목록 조회
        List<MemberRoom> memberRooms = memberRoomRepository.findByRoomIdWithMember(roomId);

        // Redis에서 준비 상태, 역할 선점 정보 조회
        Map<Long, Boolean> readyStatus = roomSessionService.getAllReadyStatus(roomId);
        Map<Long, String> roles = roomSessionService.getAllRoles(roomId);

        List<RoomMemberResponse> members = memberRooms.stream()
                .map(mr -> {
                    Member member = mr.getMember();
                    boolean isOwner = room.getOwner().getMemberId().equals(member.getMemberId());
                    boolean isReady = readyStatus.getOrDefault(member.getMemberId(), false);
                    Long roleId = findRoleIdByMemberId(roles, member.getMemberId());
                    return RoomMemberResponse.from(member, isOwner, isReady, roleId);
                })
                .collect(Collectors.toList());

        return RoomDetailResponse.from(room, members);
    }

    /**
     * 방 입장
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

        // 이미 참여 중인지 확인
        if (memberRoomRepository.existsByRoom_RoomIdAndMember_MemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_JOINED);
        }

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

        // Redis에 참여자 추가
        roomSessionService.addMember(roomId, memberId);

        log.info("방 입장 완료: roomId={}, memberId={}", roomId, memberId);
    }

    /**
     * 방 퇴장
     */
    @Transactional
    public void leaveRoom(Long roomId, Long memberId) {
        log.info("방 퇴장 시도: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 참여 중인지 확인
        MemberRoom memberRoom = memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER_ROOM));

        // MemberRoom 삭제
        memberRoomRepository.delete(memberRoom);

        // Redis에서 참여자 제거 (준비 상태, 역할도 함께 제거됨)
        roomSessionService.removeMember(roomId, memberId);

        // 방장이 나간 경우 처리
        if (room.getOwner().getMemberId().equals(memberId)) {
            handleOwnerLeave(room);
        }

        // 마지막 사람이 나간 경우 방 종료
        long remainingCount = memberRoomRepository.countByRoom_RoomId(roomId);
        if (remainingCount == 0) {
            room.updateStatus(RoomStatus.COMPLETED);
            roomSessionService.clearRoomSession(roomId);
            log.info("방 종료 (마지막 참여자 퇴장): roomId={}", roomId);
        }

        log.info("방 퇴장 완료: roomId={}, memberId={}", roomId, memberId);
    }

    /**
     * 방장 퇴장 시 처리 (위임)
     */
    private void handleOwnerLeave(Room room) {
        // 가장 먼저 입장한 사람에게 방장 위임
        memberRoomRepository.findFirstByRoomIdOrderByCreatedAtAsc(room.getRoomId())
                .ifPresent(mr -> {
                    room.updateOwner(mr.getMember());
                    log.info("방장 위임: roomId={}, newOwnerId={}",
                            room.getRoomId(), mr.getMember().getMemberId());
                });
    }

    /**
     * 역할 목록에서 특정 멤버의 역할 ID 찾기
     */
    private Long findRoleIdByMemberId(Map<Long, String> roles, Long memberId) {
        for (Map.Entry<Long, String> entry : roles.entrySet()) {
            if (memberId.toString().equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
