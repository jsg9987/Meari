package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.analysis.service.AnalysisService;
import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest;
import com.ssafy.meari.domain.room.dto.websocket.MemberSegmentInfo;
import com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage;
import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;
import com.ssafy.meari.domain.room.dto.websocket.SentenceSegmentInfo;
import com.ssafy.meari.domain.room.dto.websocket.WatchingCompleteMessage;
import com.ssafy.meari.domain.room.entity.GamePhase;
import com.ssafy.meari.domain.room.entity.MemberRoom;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import com.ssafy.meari.domain.room.repository.MemberRoomRepository;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Room 도메인의 게임 진행 워크플로우(phase 전이·녹음·라운드·분석 트리거) 전담 서비스.
 *
 * 기존 God Service였던 RoomService에서 phase 관련 책임을 분리.
 * 외부(Controller·WSController)는 RoomPhaseService를 직접 주입·호출.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomPhaseService {

    private final RoomRepository roomRepository;
    private final MemberRoomRepository memberRoomRepository;
    private final ContentRepository contentRepository;
    private final RoleRepository roleRepository;
    private final SentenceRepository sentenceRepository;
    private final ShadowingReportRepository shadowingReportRepository;
    private final RoomSessionService roomSessionService;
    private final RoomBroadcastService roomBroadcastService;
    private final AnalysisService analysisService;

    @Value("${cloud.aws.s3.bucket}")
    private String s3Bucket;

    /**
     * 게임 시작 (방장 전용)
     */
    @Transactional
    public void startGame(Long roomId, Long contentId, Long memberId) {
        log.info("게임 시작 요청: roomId={}, contentId={}, memberId={}", roomId, contentId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        // 권한·상태 검증 (도메인 메서드)
        room.checkOwnerOrThrow(memberId);
        room.checkWaitingOrThrow();

        // 콘텐츠 선택·일치 확인 (Redis)
        Long selectedContentId = roomSessionService.getContentId(roomId);
        if (selectedContentId == null) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_SELECTED);
        }
        if (!selectedContentId.equals(contentId)) {
            throw new BusinessException(ErrorCode.CONTENT_MISMATCH);
        }

        // 모든 참여자(방장 제외) 준비 완료 확인
        if (!isAllMembersReady(roomId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_ALL_READY);
        }

        // 상태 전이: WAITING → IN_PROGRESS(WATCHING)
        room.updateStatus(RoomStatus.IN_PROGRESS);
        roomSessionService.setPhase(roomId, GamePhase.WATCHING);
        roomSessionService.clearWatchingComplete(roomId);

        // 전체 스크립트(자막) 조회 및 segments 생성 + 게임 시작 브로드캐스트
        List<Sentence> sentences = sentenceRepository.findByContent_ContentId(contentId);
        List<MemberSegmentInfo> scriptSegments = buildScriptSegments(sentences);
        roomBroadcastService.broadcastState(roomId,
                RoomStateMessage.gameStart(contentId, GamePhase.WATCHING, scriptSegments));

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
            if (memberId.equals(ownerId)) {
                continue;
            }
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

        if (!room.getOwner().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.CONTENT_SELECT_ONLY_WAITING);
        }

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        long currentMemberCount = roomSessionService.getMemberCount(roomId);
        if (content.getMaxPeople() < currentMemberCount) {
            throw new BusinessException(ErrorCode.CONTENT_MAX_PEOPLE_EXCEEDED);
        }

        roomSessionService.setContent(roomId, contentId);

        RoomStateMessage message = RoomStateMessage.contentSelected(contentId);
        roomBroadcastService.broadcastState(roomId, message);

        log.info("동영상 선택 완료: roomId={}, contentId={}", roomId, contentId);
    }

    /**
     * 영상 시청 완료 (방장 전용) — WATCHING → ROLE_PICK 전환
     */
    @Transactional
    public void finishWatching(Long roomId, Long memberId) {
        log.info("영상 시청 완료 요청: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        room.checkOwnerOrThrow(memberId);
        room.checkInProgressOrThrow();

        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (currentPhase != GamePhase.WATCHING) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        roomSessionService.setPhase(roomId, GamePhase.ROLE_PICK);

        roomBroadcastService.broadcastState(roomId, RoomStateMessage.phaseChange(GamePhase.ROLE_PICK));

        log.info("영상 시청 완료, 역할 선택 단계 전환: roomId={}", roomId);
    }

    /**
     * 영상 시청 완료 (참여자 개인). 전원 완료 시 WATCHING → ROLE_PICK
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

        roomBroadcastService.broadcastState(roomId, RoomStateMessage.phaseChange(GamePhase.ROLE_PICK));
    }

    /**
     * 역할 확정 (방장 전용)
     */
    @Transactional
    public void confirmRoles(Long roomId, RoleConfirmRequest request, Long memberId) {
        log.info("역할 확정 요청: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        room.checkOwnerOrThrow(memberId);
        room.checkInProgressOrThrow();

        GamePhase phase = roomSessionService.getPhase(roomId);
        if (phase != GamePhase.ROLE_PICK) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        if (roomSessionService.isRolesConfirmed(roomId)) {
            throw new BusinessException(ErrorCode.ROLES_ALREADY_CONFIRMED);
        }

        List<MemberRoom> memberRooms = memberRoomRepository.findByRoomIdWithMember(roomId);

        if (request.getRoles().size() != memberRooms.size()) {
            throw new BusinessException(ErrorCode.ROLE_COUNT_MISMATCH);
        }

        Set<Long> memberIds = memberRooms.stream()
                .map(mr -> mr.getMember().getMemberId())
                .collect(Collectors.toSet());

        Set<Long> assignedMemberIds = new HashSet<>();
        Set<Long> assignedRoleIds = new HashSet<>();

        for (RoleConfirmRequest.RoleAssignment assignment : request.getRoles()) {
            if (!memberIds.contains(assignment.getMemberId())) {
                throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
            }
            if (!roleRepository.existsById(assignment.getRoleId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ROLE);
            }
            if (!assignedMemberIds.add(assignment.getMemberId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_MEMBER_ROLE);
            }
            if (!assignedRoleIds.add(assignment.getRoleId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_ROLE_ASSIGNMENT);
            }
        }

        roomSessionService.clearRoles(roomId);
        for (RoleConfirmRequest.RoleAssignment assignment : request.getRoles()) {
            roomSessionService.assignRole(roomId, assignment.getRoleId(), assignment.getMemberId());
        }

        roomSessionService.setRolesConfirmed(roomId, true);

        Long contentId = roomSessionService.getContentId(roomId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        List<Sentence> sentences = sentenceRepository.findByContent_ContentId(contentId);
        Map<Long, String> roleAssignments = roomSessionService.getAllRoles(roomId);

        log.debug("역할 확정 - memberRooms 수: {}, roleAssignments: {}", memberRooms.size(), roleAssignments);

        List<MemberSegmentInfo> segments = buildAllRoleSegments(roleAssignments, sentences);

        log.debug("생성된 segments 수: {}, memberIds: {}",
                segments.size(),
                segments.stream().map(MemberSegmentInfo::getMemberId).collect(Collectors.toList()));

        roomBroadcastService.broadcastState(roomId, RoomStateMessage.rolesConfirmed(segments));

        log.info("역할 확정 완료 및 segments 브로드캐스트: roomId={}, segments 수={}", roomId, segments.size());
    }

    /**
     * Round 시작 (방장 전용)
     */
    @Transactional
    public void startRound(Long roomId, Integer round, Long memberId) {
        log.info("Round 시작 요청: roomId={}, round={}, memberId={}", roomId, round, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        room.checkOwnerOrThrow(memberId);
        room.checkInProgressOrThrow();

        validateRoundStartPhase(roomId, round);

        if (!roomSessionService.isRolesConfirmed(roomId)) {
            throw new BusinessException(ErrorCode.ROLES_NOT_CONFIRMED);
        }

        Long contentId = roomSessionService.getContentId(roomId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        saveRolesToDatabase(room, content, round);

        GamePhase newPhase = round == 1 ? GamePhase.ROUND_1 : GamePhase.ROUND_2;
        roomSessionService.setPhase(roomId, newPhase);

        List<Sentence> sentences = sentenceRepository.findByContent_ContentId(contentId);
        Map<Long, String> roleAssignments = roomSessionService.getAllRoles(roomId);
        List<MemberRoom> memberRooms = memberRoomRepository.findByRoomIdWithMember(roomId);

        List<MemberSegmentInfo> segments = buildMemberSegments(memberRooms, roleAssignments, sentences);

        long playStartTime = initializeRoundSessionState(roomId, round, segments, content);

        roomBroadcastService.broadcastState(roomId,
                RoomStateMessage.roundStart(newPhase, round, playStartTime, segments));

        log.info("Round 시작 완료: roomId={}, round={}, phase={}", roomId, round, newPhase);
    }

    private void validateRoundStartPhase(Long roomId, Integer round) {
        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (round == 1 && currentPhase != GamePhase.ROLE_PICK) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }
        if (round == 2 && currentPhase != GamePhase.ROUND_1) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }
    }

    private long initializeRoundSessionState(Long roomId, Integer round,
                                             List<MemberSegmentInfo> segments, Content content) {
        for (MemberSegmentInfo segment : segments) {
            roomSessionService.setMemberTotalSentences(roomId, round, segment.getMemberId(), segment.getSentences().size());
        }
        long currentTime = System.currentTimeMillis();
        roomSessionService.setRoundStartTime(roomId, currentTime);
        long playStartTime = currentTime + 2000L;
        long timeoutMillis = playStartTime + (content.getTotalDuration().intValue() * 1000L) + 40000L;
        roomSessionService.setRoundTimeout(roomId, round, timeoutMillis);
        return playStartTime;
    }

    private List<MemberSegmentInfo> buildMemberSegments(
            List<MemberRoom> memberRooms,
            Map<Long, String> roleAssignments,
            List<Sentence> sentences
    ) {
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

        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (currentPhase != GamePhase.ROUND_1 && currentPhase != GamePhase.ROUND_2) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        if (!roomSessionService.isMember(roomId, message.getMemberId())) {
            throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
        }

        int round = currentPhase == GamePhase.ROUND_1 ? 1 : 2;

        roomSessionService.markRecordingComplete(roomId, round, message.getMemberId(), message.getSentenceId());

        if (message.getAudioUrl() != null) {
            String s3Url = convertToS3Url(message.getAudioUrl());
            roomSessionService.saveAudioUrl(roomId, round, message.getMemberId(), message.getSentenceId(), s3Url);
            log.debug("오디오 URL 변환: {} -> {}", message.getAudioUrl(), s3Url);
        }

        // 마지막 문장 메시지가 동시에 중복 도착해도 SETNX 가드로 분석 요청은 1회만 발생
        if (roomSessionService.isMemberRecordingsComplete(roomId, round, message.getMemberId())
                && roomSessionService.tryMarkAnalysisRequested(roomId, round, message.getMemberId())) {
            log.info("멤버 {} 모든 녹음 완료, 분석 요청", message.getMemberId());
            analysisService.requestMemberAnalysis(roomId, round, message.getMemberId());
        }

        if (checkAndHandleRecordingTimeout(roomId, round, currentPhase)) {
            return;
        }

        tryBroadcastRecordingsComplete(roomId, round, currentPhase);
    }

    /**
     * 영상 시청 완료 처리 (라운드별 WATCHING_COMPLETE 수신 시)
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
        roomBroadcastService.broadcastState(roomId, RoomStateMessage.recordingsComplete(currentPhase, round));
    }

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

    private List<MemberSegmentInfo> buildScriptSegments(List<Sentence> sentences) {
        Map<Long, List<Sentence>> sentencesByRole = sentences.stream()
                .collect(Collectors.groupingBy(s -> s.getRole().getRoleId()));

        return sentencesByRole.entrySet().stream()
                .map(entry -> {
                    Long roleId = entry.getKey();
                    List<Sentence> roleSentences = entry.getValue();

                    Role role = roleSentences.get(0).getRole();

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
                            .memberId(null)
                            .roleId(roleId)
                            .roleName(role.getName())
                            .sentences(segmentInfos)
                            .build();
                })
                .sorted(Comparator.comparing(MemberSegmentInfo::getRoleId))
                .collect(Collectors.toList());
    }

    private List<MemberSegmentInfo> buildAllRoleSegments(
            Map<Long, String> roleAssignments,
            List<Sentence> sentences
    ) {
        Map<Long, List<Sentence>> sentencesByRole = sentences.stream()
                .collect(Collectors.groupingBy(s -> s.getRole().getRoleId()));

        return sentencesByRole.entrySet().stream()
                .map(entry -> {
                    Long roleId = entry.getKey();
                    List<Sentence> roleSentences = entry.getValue();

                    Role role = roleSentences.get(0).getRole();

                    Long memberId = findMemberIdByRoleId(roleAssignments, roleId);

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
                            .memberId(memberId)
                            .roleId(roleId)
                            .roleName(role.getName())
                            .sentences(segmentInfos)
                            .build();
                })
                .sorted(Comparator.comparing(MemberSegmentInfo::getRoleId))
                .collect(Collectors.toList());
    }

    /**
     * 라운드 종료 (방장 전용). 부분 완료 멤버도 분석 요청.
     */
    @Transactional
    public void finishRound(Long roomId, Integer round, Long memberId) {
        log.info("라운드 종료 요청: roomId={}, round={}, memberId={}", roomId, round, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));
        room.checkOwnerOrThrow(memberId);

        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if ((round == 1 && currentPhase != GamePhase.ROUND_1) ||
            (round == 2 && currentPhase != GamePhase.ROUND_2)) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        if (roomSessionService.isRoundCompleted(roomId, round)) {
            log.info("이미 완료된 라운드: roomId={}, round={}", roomId, round);
            return;
        }

        requestPartialCompletionAnalysis(roomId, round);

        broadcastRoundComplete(roomId, round, currentPhase);

        log.info("라운드 종료 처리 완료: roomId={}, round={}", roomId, round);
    }

    /**
     * 게임 종료 및 준비 단계로 복귀 (방장 전용). Round2 종료 후 호출.
     */
    @Transactional
    public void finishGame(Long roomId, Long memberId) {
        log.info("게임 종료 요청: roomId={}, memberId={}", roomId, memberId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROOM));

        room.checkOwnerOrThrow(memberId);
        room.checkInProgressOrThrow();

        GamePhase currentPhase = roomSessionService.getPhase(roomId);
        if (currentPhase != GamePhase.ROUND_2) {
            throw new BusinessException(ErrorCode.INVALID_PHASE);
        }

        room.updateStatus(RoomStatus.WAITING);

        roomSessionService.resetGameState(roomId);

        roomBroadcastService.broadcastState(roomId, RoomStateMessage.gameFinished());

        log.info("게임 종료 완료, 준비 단계로 복귀: roomId={}", roomId);
    }

    private boolean checkAndHandleRecordingTimeout(Long roomId, Integer round, GamePhase currentPhase) {
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
            broadcastRoundComplete(roomId, round, currentPhase);
            return true;
        }

        return false;
    }

    private void requestPartialCompletionAnalysis(Long roomId, Integer round) {
        Set<String> members = roomSessionService.getMembers(roomId);
        if (members == null) {
            return;
        }

        int requestCount = 0;
        for (String memberIdStr : members) {
            Long targetMemberId = Long.parseLong(memberIdStr);

            if (roomSessionService.isMemberRecordingsComplete(roomId, round, targetMemberId)) {
                continue;
            }

            Long recordedCount = roomSessionService.getRecordedCount(roomId, round, targetMemberId);
            if (recordedCount == null || recordedCount == 0) {
                log.warn("녹음 없음 - memberId={}", targetMemberId);
                continue;
            }

            if (roomSessionService.tryMarkAnalysisRequested(roomId, round, targetMemberId)) {
                log.info("부분 완료 멤버 분석 요청 - memberId={}, recordedCount={}",
                        targetMemberId, recordedCount);
                analysisService.requestMemberAnalysis(roomId, round, targetMemberId);
                requestCount++;
            }
        }
        log.info("부분 완료 멤버 분석 요청 완료: roomId={}, round={}, 요청 수={}", roomId, round, requestCount);
    }

    private void broadcastRoundComplete(Long roomId, Integer round, GamePhase currentPhase) {
        roomSessionService.markRoundCompleted(roomId, round);
        roomBroadcastService.broadcastState(roomId, RoomStateMessage.recordingsComplete(currentPhase, round));
        log.debug("라운드 완료 브로드캐스트: roomId={}, round={}", roomId, round);
    }

    private String convertToS3Url(String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            return audioUrl;
        }
        if (audioUrl.startsWith("s3://") || audioUrl.startsWith("https://")) {
            return audioUrl;
        }
        return "s3://" + s3Bucket + "/" + audioUrl;
    }
}
