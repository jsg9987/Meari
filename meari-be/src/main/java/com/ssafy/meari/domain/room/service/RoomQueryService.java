package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.room.dto.response.RoomDetailResponse;
import com.ssafy.meari.domain.room.dto.response.RoomListResponse;
import com.ssafy.meari.domain.room.dto.response.RoomMemberResponse;
import com.ssafy.meari.domain.room.entity.GamePhase;
import com.ssafy.meari.domain.room.entity.MemberRoom;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import com.ssafy.meari.domain.room.repository.MemberRoomRepository;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.global.common.CursorPageResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Room 조회 전용 서비스 (CQRS의 Query).
 *
 * Read Only로 만들기 위해 getRoomList 내의 좀비방 정리(쓰기)는 RoomCleanupService로 위임
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomQueryService {

    private final RoomRepository roomRepository;
    private final MemberRoomRepository memberRoomRepository;
    private final ContentRepository contentRepository;
    private final RoomSessionService roomSessionService;
    private final RoomCleanupService roomCleanupService;

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
                    // (readOnly 트랜잭션이라 dirty checking flush가 안 됨 → REQUIRES_NEW 쓰기 트랜잭션에 위임)
                    if (currentPeople == 0 && room.getStatus() != RoomStatus.COMPLETED) {
                        roomCleanupService.closeAsCompleted(room.getRoomId());
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
     * 역할 목록에서 특정 멤버의 역할 ID 찾기
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
}

