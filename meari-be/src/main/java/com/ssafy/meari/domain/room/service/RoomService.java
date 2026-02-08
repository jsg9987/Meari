package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.analysis.service.AnalysisService;
import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest;
import com.ssafy.meari.domain.room.dto.request.RoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.response.RoomDetailResponse;
import com.ssafy.meari.domain.room.dto.response.RoomListResponse;
import com.ssafy.meari.domain.room.dto.response.RoomMemberResponse;
import com.ssafy.meari.domain.room.dto.response.RoomResponse;
import com.ssafy.meari.domain.room.dto.websocket.MemberSegmentInfo;
import com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage;
import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;
import com.ssafy.meari.domain.room.dto.websocket.WatchingCompleteMessage;
import com.ssafy.meari.domain.room.dto.websocket.SentenceSegmentInfo;
import com.ssafy.meari.global.common.CursorPageResponse;
import com.ssafy.meari.domain.room.entity.GamePhase;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SentenceRepository sentenceRepository;
    private final ShadowingReportRepository shadowingReportRepository;
    private final RoomSessionService roomSessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AnalysisService analysisService;

    @Value("${cloud.aws.s3.bucket}")
    private String s3Bucket;

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

        room = roomRepository.save(room);
        log.info("방 생성 완료: roomId={}", room.getRoomId());

        // 방장 자동 입장 (MemberRoom)
        MemberRoom memberRoom = MemberRoom.builder()
                .room(room)
                .member(owner)
                .build();
        memberRoomRepository.save(memberRoom);

        // Redis에 참여자 추가 및 멤버→방 매핑
        roomSessionService.addMember(room.getRoomId(), memberId);
        roomSessionService.setMemberRoom(memberId, room.getRoomId());

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

        // 각 방의 현재 Redis 기준 인원 수 및 썸네일 조회
        List<RoomListResponse> contents = rooms.stream()
                .map(room -> {
                    Set<String> members = roomSessionService.getMembers(room.getRoomId());
                    int currentPeople = (members != null) ? members.size() : 0;

                    // Redis에 아무도 없는데 방이 아직 열려있으면 → 좀비방 정리
                    if (currentPeople == 0 && room.getStatus() != RoomStatus.COMPLETED) {
                        room.updateStatus(RoomStatus.COMPLETED);
                        memberRoomRepository.deleteAllByRoom_RoomId(room.getRoomId());
                        log.info("좀비방 정리: roomId={}", room.getRoomId());
                    }

                    // 상태에 따라 썸네일 결정
                    String thumbnail;
                    if (room.getStatus() == RoomStatus.WAITING) {
                        // 대기중: 테마 썸네일
                        thumbnail = room.getTheme().getThemeUrl();
                    } else if (room.getStatus() == RoomStatus.IN_PROGRESS) {
                        // 진행중: 컨텐츠 썸네일 (없으면 테마 썸네일로 fallback)
                        Long contentId = roomSessionService.getContentId(room.getRoomId());
                        if (contentId != null) {
                            thumbnail = contentRepository.findById(contentId)
                                    .map(Content::getThumbnailUrl)
                                    .orElse(room.getTheme().getThemeUrl());
                        } else {
                            thumbnail = room.getTheme().getThemeUrl();
                        }
                    } else {
                        // COMPLETED 등 기타 상태: 테마 썸네일
                        thumbnail = room.getTheme().getThemeUrl();
                    }

                    return RoomListResponse.from(room, currentPeople, thumbnail);
                })
                .collect(Collectors.toList());

        Long nextCursor = hasNext && !rooms.isEmpty()
                ? rooms.get(rooms.size() - 1).getRoomId()
                : null;

        return CursorPageResponse.of(contents, nextCursor, hasNext);
    }

    /**
     * 방 상세 조회
     * Redis의 실제 참여자로 필터링하여 이미 나간 사람이 보이지 않도록 처리
     */
    public RoomDetailResponse getRoomDetail(Long roomId) {
        log.info("방 상세 조회: roomId={}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // DB에서 모든 참여 기록 조회
        List<MemberRoom> allMemberRooms = memberRoomRepository.findByRoomIdWithMember(roomId);

        // Redis의 현재 실제 참여자 조회 (연결이 끊기지 않은 사람들)
        Set<String> redisMembers = roomSessionService.getMembers(roomId);

        // DB 데이터를 Redis로 필터링 (실제 참여 중인 멤버만 남김)
        // 이렇게 하면 나갔을 때 leaveRoom이 호출되지 않은 경우에도
        // Redis의 실제 상태를 기준으로 표시됨
        List<MemberRoom> memberRooms = allMemberRooms.stream()
                .filter(mr -> redisMembers != null &&
                             redisMembers.contains(mr.getMember().getMemberId().toString()))
                .collect(Collectors.toList());

        log.debug("방 상세 조회 필터링: 전체={}, 필터 후={}", allMemberRooms.size(), memberRooms.size());

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

        // Redis에서 게임 상태 정보 조회
        Long contentId = roomSessionService.getContentId(roomId);
        GamePhase phase = roomSessionService.getPhase(roomId);
        Boolean rolesConfirmed = roomSessionService.isRolesConfirmed(roomId) ? true : null;

        return RoomDetailResponse.from(room, members, contentId, phase, rolesConfirmed);
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
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

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
     * 멤버 강퇴 (방장 전용, WAITING 상태에서만)
     */
    @Transactional
    public void kickMember(Long roomId, Long targetMemberId, Long requestMemberId) {
        log.info("멤버 강퇴 요청: roomId={}, targetMemberId={}, requestMemberId={}", roomId, targetMemberId, requestMemberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // WAITING 상태 확인
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.ROOM_NOT_WAITING);
        }

        // 방장 권한 확인
        if (!room.getOwner().getMemberId().equals(requestMemberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }

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
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

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
        if (remainingCount == 0) {
            room.updateStatus(RoomStatus.COMPLETED);
            roomSessionService.clearRoomSession(roomId);
            log.info("비정상 종료: 마지막 사람 퇴장으로 방 종료, roomId={}", roomId);
        }

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
        roomSessionService.setPhase(roomId, GamePhase.WATCHING);
        roomSessionService.clearWatchingComplete(roomId);

        // 전체 스크립트(자막) 조회 및 segments 생성
        List<Sentence> sentences = sentenceRepository.findByContent_ContentId(contentId);
        List<MemberSegmentInfo> scriptSegments = buildScriptSegments(sentences);

        // 게임 시작 알림 브로드캐스트 (contentId + phase + 전체 자막)
        RoomStateMessage message = RoomStateMessage.gameStart(contentId, GamePhase.WATCHING, scriptSegments);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("게임 시작 완료: roomId={}, contentId={}, 전체 스크립트 segments 수={}",
                roomId, contentId, scriptSegments.size());
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

        // 콘텐츠 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        // 현재 참여자 수가 콘텐츠 최대 인원을 초과하는지 확인
        long currentMemberCount = roomSessionService.getMemberCount(roomId);
        if (content.getMaxPeople() < currentMemberCount) {
            throw new BusinessException(ErrorCode.CONTENT_MAX_PEOPLE_EXCEEDED);
        }

        // Redis에 콘텐츠 저장 (phase는 변경하지 않음)
        roomSessionService.setContent(roomId, contentId);

        // 동영상 선택 알림 브로드캐스트
        RoomStateMessage message = RoomStateMessage.contentSelected(contentId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("동영상 선택 완료: roomId={}, contentId={}", roomId, contentId);
    }

    /**
     * 영상 시청 완료 (방장 전용)
     * WATCHING → ROLE_PICK phase 전환
     */
    @Transactional
    public void finishWatching(Long roomId, Long memberId) {
        log.info("영상 시청 완료 요청: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 방장 권한 확인
        if (!room.getOwner().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }

        // 진행 중인 방인지 확인
        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.ROOM_NOT_IN_PROGRESS);
        }

        // 현재 phase가 WATCHING인지 확인
        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (currentPhase != GamePhase.WATCHING) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        // phase를 ROLE_PICK으로 전환
        roomSessionService.setPhase(roomId, GamePhase.ROLE_PICK);

        // 브로드캐스트
        RoomStateMessage message = RoomStateMessage.phaseChange(GamePhase.ROLE_PICK);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("영상 시청 완료, 역할 선택 단계 전환: roomId={}", roomId);
    }

    /**
     * 영상 시청 완료 (참여자 개인)
     * 각 참여자가 시청 완료 시 호출. 4명 모두 완료 시 WATCHING → ROLE_PICK 전환 및 브로드캐스트
     */
    @Transactional
    public void watchingComplete(Long roomId, Long memberId) {
        log.info("영상 시청 완료 수신: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.ROOM_NOT_IN_PROGRESS);
        }

        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (currentPhase != GamePhase.WATCHING) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        if (!roomSessionService.isMember(roomId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
        }

        roomSessionService.markWatchingComplete(roomId, memberId);

        if (!roomSessionService.isAllWatchingComplete(roomId)) {
            log.debug("영상 시청 완료: 아직 전체 미완료, roomId={}", roomId);
            return;
        }

        log.info("모든 참여자 영상 시청 완료, 역할 선택 단계 전환: roomId={}", roomId);
        roomSessionService.clearWatchingComplete(roomId);
        roomSessionService.setPhase(roomId, GamePhase.ROLE_PICK);

        RoomStateMessage message = RoomStateMessage.phaseChange(GamePhase.ROLE_PICK);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);
    }

    /**
     * 역할 선택 완료 (방장 전용)
     * 프론트엔드에서 최종 확정된 역할 데이터를 받아 검증 후 Redis에 저장
     */
    @Transactional
    public void confirmRoles(Long roomId, RoleConfirmRequest request, Long memberId) {
        log.info("역할 확정 요청: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        if (!room.getOwner().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }

        // 진행 중인 방인지 확인
        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.ROOM_NOT_IN_PROGRESS);
        }

        GamePhase phase = roomSessionService.getPhase(roomId);
        if (phase != GamePhase.ROLE_PICK) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        // 이미 확정되었는지 확인
        if (roomSessionService.isRolesConfirmed(roomId)) {
            throw new BusinessException(ErrorCode.ROLES_ALREADY_CONFIRMED);
        }

        // 현재 방 참여자 목록 조회
        List<MemberRoom> memberRooms = memberRoomRepository.findByRoomIdWithMember(roomId);

        // 1. 모든 참여자가 역할을 할당받았는지 확인
        if (request.getRoles().size() != memberRooms.size()) {
            throw new BusinessException(ErrorCode.ROLE_COUNT_MISMATCH);
        }

        // 2. 참여자 및 역할 검증
        Set<Long> memberIds = memberRooms.stream()
                .map(mr -> mr.getMember().getMemberId())
                .collect(java.util.stream.Collectors.toSet());

        Set<Long> assignedMemberIds = new HashSet<>();
        Set<Long> assignedRoleIds = new HashSet<>();

        for (RoleConfirmRequest.RoleAssignment assignment : request.getRoles()) {
            // 참여 중인 멤버인지 확인
            if (!memberIds.contains(assignment.getMemberId())) {
                throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
            }

            // 역할 ID 유효성 확인
            if (!roleRepository.existsById(assignment.getRoleId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ROLE);
            }

            // 중복 할당 확인
            if (!assignedMemberIds.add(assignment.getMemberId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_MEMBER_ROLE);
            }
            if (!assignedRoleIds.add(assignment.getRoleId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_ROLE_ASSIGNMENT);
            }
        }

        // 3. Redis roles 초기화 후 최종 데이터로 업데이트
        roomSessionService.clearRoles(roomId);
        for (RoleConfirmRequest.RoleAssignment assignment : request.getRoles()) {
            roomSessionService.assignRole(roomId, assignment.getRoleId(), assignment.getMemberId());
        }

        // 4. 확정 플래그 설정
        roomSessionService.setRolesConfirmed(roomId, true);

        // 5. 전체 segments 정보 생성 및 브로드캐스트
        Long contentId = roomSessionService.getContentId(roomId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        List<Sentence> sentences = sentenceRepository.findByContent_ContentId(contentId);
        Map<Long, String> roleAssignments = roomSessionService.getAllRoles(roomId);

        log.debug("역할 확정 - memberRooms 수: {}, roleAssignments: {}", memberRooms.size(), roleAssignments);

        // 모든 역할(할당된 역할 + 시스템 역할)의 segments 생성
        List<MemberSegmentInfo> segments = buildAllRoleSegments(roleAssignments, sentences);

        log.debug("생성된 segments 수: {}, memberIds: {}",
                segments.size(),
                segments.stream().map(MemberSegmentInfo::getMemberId).collect(java.util.stream.Collectors.toList()));

        RoomStateMessage message = RoomStateMessage.rolesConfirmed(segments);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("역할 확정 완료 및 segments 브로드캐스트: roomId={}, segments 수={} (시스템 역할 포함)", roomId, segments.size());
    }

    /**
     * Round 시작 (방장 전용)
     * Round1 시작 시 역할 정보 DB 저장
     * Sentence 조회, 멤버-역할 매핑, ROUND_START 브로드캐스트
     */
    @Transactional
    public void startRound(Long roomId, Integer round, Long memberId) {
        log.info("Round 시작 요청: roomId={}, round={}, memberId={}", roomId, round, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        if (!room.getOwner().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }

        // 진행 중인 방인지 확인
        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.ROOM_NOT_IN_PROGRESS);
        }

        // 현재 phase 검증
        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (round == 1) {
            if (currentPhase != GamePhase.ROLE_PICK) {
                throw new BusinessException(ErrorCode.INVALID_PHASE);
            }
        } else if (round == 2) {
            if (currentPhase != GamePhase.ROUND_1) {
                throw new BusinessException(ErrorCode.INVALID_PHASE);
            }
        }

        // 역할이 확정되었는지 확인
        if (!roomSessionService.isRolesConfirmed(roomId)) {
            throw new BusinessException(ErrorCode.ROLES_NOT_CONFIRMED);
        }

        Long contentId = roomSessionService.getContentId(roomId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        // 라운드별 ShadowingReport 생성 (Round 1, 2 모두)
        saveRolesToDatabase(room, content, round);

        // phase 변경
        GamePhase newPhase = round == 1 ? GamePhase.ROUND_1 : GamePhase.ROUND_2;
        roomSessionService.setPhase(roomId, newPhase);

        // Sentence 조회 및 멤버별 세그먼트 구성
        List<Sentence> sentences = sentenceRepository.findByContent_ContentId(contentId);
        Map<Long, String> roleAssignments = roomSessionService.getAllRoles(roomId);
        List<MemberRoom> memberRooms = memberRoomRepository.findByRoomIdWithMember(roomId);

        List<MemberSegmentInfo> segments = buildMemberSegments(memberRooms, roleAssignments, sentences);

        // 각 멤버별 예상 문장수 저장
        for (MemberSegmentInfo segment : segments) {
            roomSessionService.setMemberTotalSentences(roomId, round, segment.getMemberId(), segment.getSentences().size());
        }

        // Round 시작 시각 저장 (타임아웃 계산용)
        long currentTime = System.currentTimeMillis();
        roomSessionService.setRoundStartTime(roomId, currentTime);

        // 실제 재생 시작 시간 (현재 시간 + 2초)
        long playStartTime = currentTime + 2000L;

        // 타임아웃 시간 계산 및 저장 (재생 시작 시간 + 영상 길이 + 40초)
        int videoDurationSeconds = content.getTotalDuration().intValue();
        long timeoutMillis = playStartTime + (videoDurationSeconds * 1000L) + 40000L;
        roomSessionService.setRoundTimeout(roomId, round, timeoutMillis);

        // ROUND_START 브로드캐스트 (재생 시작 시간 전달)
        RoomStateMessage message = RoomStateMessage.roundStart(newPhase, round, playStartTime, segments);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("Round 시작 완료: roomId={}, round={}, phase={}, 재생시작={}ms 후, 타임아웃={}초",
                roomId, round, newPhase, 2, videoDurationSeconds + 40);
    }

    /**
     * 멤버별 문장 세그먼트 구성
     */
    private List<MemberSegmentInfo> buildMemberSegments(
            List<MemberRoom> memberRooms,
            Map<Long, String> roleAssignments,
            List<Sentence> sentences
    ) {
        // roleId → List<Sentence> 매핑
        Map<Long, List<Sentence>> sentencesByRole = sentences.stream()
                .collect(Collectors.groupingBy(s -> s.getRole().getRoleId()));

        return memberRooms.stream()
                .map(mr -> {
                    Long mId = mr.getMember().getMemberId();
                    Long roleId = findRoleIdByMemberId(roleAssignments, mId);

                    if (roleId == null) {
                        throw new BusinessException(ErrorCode.ROLE_NOT_SELECTED);
                    }

                    Role role = roleRepository.findById(roleId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROLE));

                    List<SentenceSegmentInfo> memberSentences = sentencesByRole.getOrDefault(roleId, List.of())
                            .stream()
                            .map(s -> SentenceSegmentInfo.builder()
                                    .sentenceId(s.getSentenceId())
                                    .sequence(s.getSequence())
                                    .startTime(s.getStartTime().doubleValue())
                                    .endTime(s.getEndTime().doubleValue())
                                    .textKo(s.getTextKo())
                                    .textVn(s.getTextVn())
                                    .build())
                            .sorted(Comparator.comparingInt(SentenceSegmentInfo::getSequence))
                            .collect(Collectors.toList());

                    return MemberSegmentInfo.builder()
                            .memberId(mId)
                            .roleId(roleId)
                            .roleName(role.getName())
                            .sentences(memberSentences)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 문장별 녹음 완료 처리
     */
    @Transactional
    public void recordingComplete(Long roomId, RecordingCompleteMessage message) {
        log.info("녹음 완료 요청: roomId={}, memberId={}, sentenceId={}, audioUrl={}",
                roomId, message.getMemberId(), message.getSentenceId(), message.getAudioUrl());

        // 현재 phase가 ROUND인지 확인
        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (currentPhase != GamePhase.ROUND_1 && currentPhase != GamePhase.ROUND_2) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        // 방의 멤버인지 확인
        if (!roomSessionService.isMember(roomId, message.getMemberId())) {
            throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
        }

        int round = currentPhase == GamePhase.ROUND_1 ? 1 : 2;

        // 문장 녹음 완료 마킹
        roomSessionService.markRecordingComplete(roomId, round, message.getMemberId(), message.getSentenceId());

        // 오디오 URL 저장 (S3 URL 형식으로 변환)
        if (message.getAudioUrl() != null) {
            String s3Url = convertToS3Url(message.getAudioUrl());
            roomSessionService.saveAudioUrl(roomId, round, message.getMemberId(), message.getSentenceId(), s3Url);
            log.debug("오디오 URL 변환: {} -> {}", message.getAudioUrl(), s3Url);
        }

        // 이 멤버의 모든 문장이 완료되었는지 체크
        if (roomSessionService.isMemberRecordingsComplete(roomId, round, message.getMemberId())) {
            log.info("멤버 {} 모든 녹음 완료, 분석 요청", message.getMemberId());

            // 멤버별 발음 분석 요청 (비동기)
            analysisService.requestMemberAnalysis(roomId, round, message.getMemberId());
        }

        // 타임아웃 체크
        if (checkAndHandleRecordingTimeout(roomId, round, currentPhase)) {
            return; // 타임아웃 처리됨
        }

        // 전원 영상 시청 완료 + 전원 녹음 전송 완료 시 영상 시청 끝남 브로드캐스트
        tryBroadcastRecordingsComplete(roomId, round, currentPhase);
    }

    /**
     * 영상 시청 완료 처리 (WATCHING_COMPLETE 수신 시)
     * 전원 WATCHING_COMPLETE + 전원 녹음 전송 완료 시 RECORDINGS_COMPLETE 브로드캐스트
     */
    @Transactional
    public void watchingComplete(Long roomId, WatchingCompleteMessage message) {
        log.info("영상 시청 완료 요청: roomId={}, memberId={}", roomId, message.getMemberId());

        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (currentPhase != GamePhase.ROUND_1 && currentPhase != GamePhase.ROUND_2) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }
        if (!roomSessionService.isMember(roomId, message.getMemberId())) {
            throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
        }

        int round = currentPhase == GamePhase.ROUND_1 ? 1 : 2;
        roomSessionService.addMemberWatchingComplete(roomId, round, message.getMemberId());

        if (checkAndHandleRecordingTimeout(roomId, round, currentPhase)) {
            return;
        }

        tryBroadcastRecordingsComplete(roomId, round, currentPhase);
    }

    /**
     * 전원 영상 시청 완료 + 전원 녹음 전송 완료 시 RECORDINGS_COMPLETE 브로드캐스트 (중복 방지)
     */
    private void tryBroadcastRecordingsComplete(Long roomId, int round, GamePhase currentPhase) {
        if (roomSessionService.isRoundCompleted(roomId, round)) {
            return;
        }
        if (!roomSessionService.isAllWatchingComplete(roomId, round)
                || !roomSessionService.isAllRecordingsComplete(roomId, round)) {
            return;
        }
        log.info("모든 멤버 영상 시청 및 녹음 전송 완료: roomId={}, round={}", roomId, round);
        roomSessionService.markRoundCompleted(roomId, round);
        RoomStateMessage completeMessage = RoomStateMessage.recordingsComplete(currentPhase, round);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", completeMessage);
    }

    /**
     * Redis 역할 정보를 DB에 저장 (라운드별 ShadowingReport 생성)
     */
    private void saveRolesToDatabase(Room room, Content content, Integer round) {
        List<MemberRoom> memberRooms = memberRoomRepository.findByRoomIdWithMember(room.getRoomId());
        Map<Long, String> roles = roomSessionService.getAllRoles(room.getRoomId());

        for (MemberRoom mr : memberRooms) {
            Long roleId = findRoleIdByMemberId(roles, mr.getMember().getMemberId());
            if (roleId == null) {
                throw new BusinessException(ErrorCode.ROLE_NOT_SELECTED);
            }
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROLE));

            shadowingReportRepository.save(ShadowingReport.builder()
                    .member(mr.getMember())
                    .room(room)
                    .role(role)
                    .content(content)
                    .round(round)
                    .build());
        }
        log.info("라운드별 ShadowingReport 생성 완료: roomId={}, round={}, 멤버 수={}",
                room.getRoomId(), round, memberRooms.size());
    }

    /**
     * 역할 목록에서 특정 멤버의 역할 ID 찾기
     * @return 역할 ID 또는 null
     */
    private Long findRoleIdByMemberId(Map<Long, String> roles, Long memberId) {
        if (roles == null) {
            return null;
        }
        for (Map.Entry<Long, String> entry : roles.entrySet()) {
            if (memberId.toString().equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 역할 할당 정보에서 특정 roleId에 할당된 memberId 찾기
     * @return memberId 또는 null (시스템 역할인 경우)
     */
    private Long findMemberIdByRoleId(Map<Long, String> roleAssignments, Long roleId) {
        if (roleAssignments == null) {
            return null;
        }
        String value = roleAssignments.get(roleId);
        if (value == null || "SYSTEM".equals(value)) {
            return null;
        }
        return Long.parseLong(value);
    }

    /**
     * 전체 스크립트 세그먼트 구성 (영상 시청용, 역할 할당 없음)
     * 모든 역할의 memberId는 null로 설정
     */
    private List<MemberSegmentInfo> buildScriptSegments(List<Sentence> sentences) {
        // roleId → List<Sentence> 매핑
        Map<Long, List<Sentence>> sentencesByRole = sentences.stream()
                .collect(Collectors.groupingBy(s -> s.getRole().getRoleId()));

        return sentencesByRole.entrySet().stream()
                .map(entry -> {
                    Long roleId = entry.getKey();
                    List<Sentence> roleSentences = entry.getValue();

                    // Role 정보 조회
                    Role role = roleSentences.get(0).getRole();

                    // 문장 정보 생성
                    List<SentenceSegmentInfo> segmentInfos = roleSentences.stream()
                            .map(s -> SentenceSegmentInfo.builder()
                                    .sentenceId(s.getSentenceId())
                                    .sequence(s.getSequence())
                                    .startTime(s.getStartTime().doubleValue())
                                    .endTime(s.getEndTime().doubleValue())
                                    .textKo(s.getTextKo())
                                    .textVn(s.getTextVn())
                                    .build())
                            .sorted(Comparator.comparingInt(SentenceSegmentInfo::getSequence))
                            .collect(Collectors.toList());

                    return MemberSegmentInfo.builder()
                            .memberId(null)  // 영상 시청 단계에서는 역할 미할당
                            .roleId(roleId)
                            .roleName(role.getName())
                            .sentences(segmentInfos)
                            .build();
                })
                .sorted(Comparator.comparing(MemberSegmentInfo::getRoleId))
                .collect(Collectors.toList());
    }

    /**
     * 모든 역할에 대한 세그먼트 구성 (할당된 역할 + 시스템 역할 모두 포함)
     * 프론트엔드에서 모든 타임스탬프 정보를 처리하기 위해 사용
     */
    private List<MemberSegmentInfo> buildAllRoleSegments(
            Map<Long, String> roleAssignments,
            List<Sentence> sentences
    ) {
        // roleId → List<Sentence> 매핑
        Map<Long, List<Sentence>> sentencesByRole = sentences.stream()
                .collect(Collectors.groupingBy(s -> s.getRole().getRoleId()));

        return sentencesByRole.entrySet().stream()
                .map(entry -> {
                    Long roleId = entry.getKey();
                    List<Sentence> roleSentences = entry.getValue();

                    // Role 정보 조회 (첫 번째 문장에서 가져옴)
                    Role role = roleSentences.get(0).getRole();

                    // 할당된 memberId 찾기 (없으면 null = 시스템 역할)
                    Long memberId = findMemberIdByRoleId(roleAssignments, roleId);

                    // 문장 정보 생성
                    List<SentenceSegmentInfo> segmentInfos = roleSentences.stream()
                            .map(s -> SentenceSegmentInfo.builder()
                                    .sentenceId(s.getSentenceId())
                                    .sequence(s.getSequence())
                                    .startTime(s.getStartTime().doubleValue())
                                    .endTime(s.getEndTime().doubleValue())
                                    .textKo(s.getTextKo())
                                    .textVn(s.getTextVn())
                                    .build())
                            .sorted(Comparator.comparingInt(SentenceSegmentInfo::getSequence))
                            .collect(Collectors.toList());

                    return MemberSegmentInfo.builder()
                            .memberId(memberId)  // null이면 시스템 역할
                            .roleId(roleId)
                            .roleName(role.getName())
                            .sentences(segmentInfos)
                            .build();
                })
                .sorted(Comparator.comparing(MemberSegmentInfo::getRoleId))
                .collect(Collectors.toList());
    }

    /**
     * 라운드 종료 (방장 전용)
     * 부분 완료 멤버도 분석 요청 발송
     */
    @Transactional
    public void finishRound(Long roomId, Integer round, Long memberId) {
        log.info("라운드 종료 요청: roomId={}, round={}, memberId={}", roomId, round, memberId);

        // 1. 방장 권한 확인
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));
        if (!room.getOwner().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }

        // 2. Phase 확인
        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if ((round == 1 && currentPhase != GamePhase.ROUND_1) ||
            (round == 2 && currentPhase != GamePhase.ROUND_2)) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        // 3. 이미 완료된 라운드인지 확인
        if (roomSessionService.isRoundCompleted(roomId, round)) {
            log.info("이미 완료된 라운드: roomId={}, round={}", roomId, round);
            return;
        }

        // 4. 부분 완료 멤버 분석 요청
        requestPartialCompletionAnalysis(roomId, round);

        // 5. 완료 플래그 설정 및 브로드캐스트
        broadcastRoundComplete(roomId, round, currentPhase);

        log.info("라운드 종료 처리 완료: roomId={}, round={}", roomId, round);
    }

    /**
     * 게임 종료 및 준비 단계로 복귀 (방장 전용)
     * Round2 종료 후 호출
     */
    @Transactional
    public void finishGame(Long roomId, Long memberId) {
        log.info("게임 종료 요청: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 방장 권한 확인
        if (!room.getOwner().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }

        // 진행 중인 방인지 확인
        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.ROOM_NOT_IN_PROGRESS);
        }

        // Round2 단계에서만 종료 가능
        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (currentPhase != GamePhase.ROUND_2) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        // 방 상태를 WAITING으로 변경
        room.updateStatus(RoomStatus.WAITING);

        // Redis 게임 상태 초기화 (phase 삭제 포함)
        roomSessionService.resetGameState(roomId);

        // 게임 종료 브로드캐스트 (프론트엔드에서 준비 단계로 복귀)
        RoomStateMessage message = RoomStateMessage.gameFinished();
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", message);

        log.info("게임 종료 완료, 준비 단계로 복귀: roomId={}", roomId);
    }

    /**
     * 녹음 완료 타임아웃 체크
     * 타임아웃 시 강제 종료 (브로드캐스트만 수행)
     * @return true: 타임아웃 처리됨, false: 타임아웃 아님
     */
    private boolean checkAndHandleRecordingTimeout(Long roomId, Integer round, GamePhase currentPhase) {
        // 이미 완료 처리되었는지 확인
        if (roomSessionService.isRoundCompleted(roomId, round)) {
            return true;
        }

        Long timeoutMillis = roomSessionService.getRoundTimeout(roomId, round);
        if (timeoutMillis == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime > timeoutMillis) {
            log.warn("녹음 완료 타임아웃: roomId={}, round={}, 강제 종료 처리", roomId, round);

            // 타임아웃 시에는 브로드캐스트만 수행 (부분 완료 처리는 finishRound에서만)
            broadcastRoundComplete(roomId, round, currentPhase);

            return true;
        }

        return false;
    }

    /**
     * 부분 완료 멤버 분석 요청
     * finishRound()에서만 호출됨
     */
    private void requestPartialCompletionAnalysis(Long roomId, Integer round) {
        Set<String> members = roomSessionService.getMembers(roomId);
        if (members == null) {
            return;
        }

        int requestCount = 0;
        for (String memberIdStr : members) {
            Long targetMemberId = Long.parseLong(memberIdStr);

            // 이미 모든 문장 완료한 경우는 건너뛰기 (이미 분석 요청됨)
            if (roomSessionService.isMemberRecordingsComplete(roomId, round, targetMemberId)) {
                continue;
            }

            // 1개 이상 녹음했으면 분석 요청
            Long recordedCount = roomSessionService.getRecordedCount(roomId, round, targetMemberId);
            if (recordedCount != null && recordedCount > 0) {
                log.info("부분 완료 멤버 분석 요청 - memberId={}, recordedCount={}",
                        targetMemberId, recordedCount);
                analysisService.requestMemberAnalysis(roomId, round, targetMemberId);
                requestCount++;
            } else {
                log.warn("녹음 없음 - memberId={}", targetMemberId);
            }
        }
        log.info("부분 완료 멤버 분석 요청 완료: roomId={}, round={}, 요청 수={}", roomId, round, requestCount);
    }

    /**
     * 라운드 완료 브로드캐스트
     * finishRound() 및 타임아웃 처리에서 공통으로 호출
     */
    private void broadcastRoundComplete(Long roomId, Integer round, GamePhase currentPhase) {
        roomSessionService.markRoundCompleted(roomId, round);
        RoomStateMessage completeMessage = RoomStateMessage.recordingsComplete(currentPhase, round);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", completeMessage);
        log.debug("라운드 완료 브로드캐스트: roomId={}, round={}", roomId, round);
    }

    /**
     * S3 URL 형식 변환
     * - Key만 들어온 경우: s3://bucket/key 형식으로 변환
     * - 이미 s3:// 또는 https:// 형식인 경우: 그대로 반환
     *
     * @param audioUrl 원본 URL 또는 S3 Key
     * @return s3:// 형식의 URL
     */
    private String convertToS3Url(String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            return audioUrl;
        }

        // 이미 s3:// 또는 https:// 형식이면 그대로 반환
        if (audioUrl.startsWith("s3://") || audioUrl.startsWith("https://")) {
            return audioUrl;
        }

        // Key만 있는 경우 s3:// 형식으로 변환
        return "s3://" + s3Bucket + "/" + audioUrl;
    }
}
