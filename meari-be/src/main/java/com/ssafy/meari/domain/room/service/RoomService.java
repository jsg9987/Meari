package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.room.dto.request.RoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.response.RoomDetailResponse;
import com.ssafy.meari.domain.room.dto.response.RoomListResponse;
import com.ssafy.meari.domain.room.dto.response.RoomMemberResponse;
import com.ssafy.meari.domain.room.dto.response.RoomResponse;
import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final ContentRepository contentRepository;
    private final RoleRepository roleRepository;
    private final RoomSessionService roomSessionService;
    private final SimpMessagingTemplate messagingTemplate;

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

        // 입장 알림 브로드캐스트
        RoomStateMessage message = RoomStateMessage.memberJoin(memberId, member.getNickname());
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

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
            log.info("방 종료 (마지막 참여자 퇴장): roomId={}", roomId);
        }

        log.info("방 퇴장 완료: roomId={}, memberId={}", roomId, memberId);
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
     * 준비 상태 토글
     * @return 변경 후 준비 상태
     */
    public boolean toggleReady(Long roomId, Long memberId) {
        log.info("준비 상태 토글: roomId={}, memberId={}", roomId, memberId);

        // 방 존재 확인
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 방장은 준비 상태 변경 불가 (항상 준비 완료 상태)
        if (room.getOwner().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_READY);
        }

        // 참여 중인지 확인
        if (!memberRoomRepository.existsByRoom_RoomIdAndMember_MemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 현재 준비 상태 조회 후 토글
        boolean currentReady = roomSessionService.isReady(roomId, memberId);
        boolean newReady = !currentReady;
        roomSessionService.setReady(roomId, memberId, newReady);

        // 준비 상태 변경 알림 브로드캐스트
        RoomStateMessage message = RoomStateMessage.ready(memberId, newReady);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("준비 상태 변경 완료: roomId={}, memberId={}, ready={}", roomId, memberId, newReady);
        return newReady;
    }

    /**
     * 게임 시작 (방장 전용)
     */
    @Transactional
    public void startGame(Long roomId, Long contentId, Long memberId) {
        log.info("게임 시작 요청: roomId={}, contentId={}, memberId={}", roomId, contentId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 방장 권한 확인
        if (!room.getOwner().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }

        // 이미 진행 중인지 확인
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.ROOM_NOT_WAITING);
        }

        // 동영상이 선택되었는지 확인 (Redis의 contentId와 일치해야 함)
        Long selectedContentId = roomSessionService.getContentId(roomId);
        if (selectedContentId == null) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_SELECTED);
        }
        if (!selectedContentId.equals(contentId)) {
            throw new BusinessException(ErrorCode.CONTENT_MISMATCH);
        }

        // 모든 참여자(방장 제외)가 준비 완료인지 확인
        if (!isAllMembersReady(roomId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_ALL_READY);
        }

        // 방 상태 변경
        room.updateStatus(RoomStatus.IN_PROGRESS);
        roomSessionService.setPhase(roomId, "WATCHING");

        // 게임 시작 알림 브로드캐스트 (contentId + phase)
        RoomStateMessage message = RoomStateMessage.gameStart(contentId, "WATCHING");
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("게임 시작 완료: roomId={}, contentId={}", roomId, contentId);
    }

    /**
     * 방장 제외 모든 참여자가 준비 완료인지 확인
     */
    private boolean isAllMembersReady(Long roomId, Long ownerId) {
        List<MemberRoom> memberRooms = memberRoomRepository.findByRoomIdWithMember(roomId);

        for (MemberRoom mr : memberRooms) {
            Long memberId = mr.getMember().getMemberId();
            // 방장은 제외
            if (memberId.equals(ownerId)) {
                continue;
            }
            // 준비 안 된 참여자가 있으면 false
            if (!roomSessionService.isReady(roomId, memberId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 동영상 선택 (방장 전용)
     */
    @Transactional
    public void selectContent(Long roomId, Long contentId, Long memberId) {
        log.info("동영상 선택 요청: roomId={}, contentId={}, memberId={}", roomId, contentId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 방장 권한 확인
        if (!room.getOwner().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }

        // WAITING 상태인지 확인 (게임 시작 전에만 선택 가능)
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.CONTENT_SELECT_ONLY_WAITING);
        }

        // 콘텐츠 존재 확인
        if (!contentRepository.existsById(contentId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_CONTENT);
        }

        // Redis에 콘텐츠 저장 (phase는 변경하지 않음)
        roomSessionService.setContent(roomId, contentId);

        // 동영상 선택 알림 브로드캐스트
        RoomStateMessage message = RoomStateMessage.contentSelected(contentId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("동영상 선택 완료: roomId={}, contentId={}", roomId, contentId);
    }

    /**
     * 역할 선점
     */
    public void selectRole(Long roomId, Long roleId, Long memberId) {
        log.info("역할 선점 요청: roomId={}, roleId={}, memberId={}", roomId, roleId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 참여 중인지 확인
        if (!memberRoomRepository.existsByRoom_RoomIdAndMember_MemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 진행 중 상태인지 확인
        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.ROOM_NOT_IN_PROGRESS);
        }

        // ROLE_PICK 단계인지 확인
        String phase = roomSessionService.getPhase(roomId);
        if (!"ROLE_PICK".equals(phase)) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        // 역할 존재 확인
        if (!roleRepository.existsById(roleId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ROLE);
        }

        // Redis로 원자적 선점 시도
        boolean success = roomSessionService.tryAssignRole(roomId, roleId, memberId);
        if (!success) {
            throw new BusinessException(ErrorCode.ROLE_ALREADY_TAKEN);
        }

        // 역할 선점 알림 브로드캐스트
        RoomStateMessage message = RoomStateMessage.roleAssigned(memberId, roleId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("역할 선점 완료: roomId={}, roleId={}, memberId={}", roomId, roleId, memberId);
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
