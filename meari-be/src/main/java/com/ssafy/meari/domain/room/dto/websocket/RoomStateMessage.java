package com.ssafy.meari.domain.room.dto.websocket;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.room.entity.GamePhase;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 방 상태 변경 브로드캐스트 메시지
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoomStateMessage {

    private String type;      // MEMBER_JOIN, MEMBER_LEAVE, READY, ROLE_ASSIGNED, ROLE_RELEASED, PHASE_CHANGE, CONTENT_SELECTED, ROUND_START, RECORDINGS_COMPLETE
    private Long memberId;
    private Boolean ready;
    private Long roleId;
    private GamePhase phase;
    private String nickname;
    private Long newOwnerId;  // 방장 변경 시
    private Long contentId;   // 동영상 선택 시
    private Integer round;    // Round 시작/완료 시
    private Long serverTime;  // Round 시작 시각 (epoch millis)
    private List<MemberSegmentInfo> segments; // Round 시작 시 멤버별 문장 세그먼트

    public static RoomStateMessage memberJoin(Long memberId, String nickname) {
        return RoomStateMessage.builder()
                .type("MEMBER_JOIN")
                .memberId(memberId)
                .nickname(nickname)
                .build();
    }

    public static RoomStateMessage memberLeave(Long memberId, Long newOwnerId) {
        return RoomStateMessage.builder()
                .type("MEMBER_LEAVE")
                .memberId(memberId)
                .newOwnerId(newOwnerId)
                .build();
    }

    public static RoomStateMessage ready(Long memberId, boolean ready) {
        return RoomStateMessage.builder()
                .type("READY")
                .memberId(memberId)
                .ready(ready)
                .build();
    }

    public static RoomStateMessage roleAssigned(Long memberId, Long roleId) {
        return RoomStateMessage.builder()
                .type("ROLE_ASSIGNED")
                .memberId(memberId)
                .roleId(roleId)
                .build();
    }

    public static RoomStateMessage roleReleased(Long memberId, Long roleId) {
        return RoomStateMessage.builder()
                .type("ROLE_RELEASED")
                .memberId(memberId)
                .roleId(roleId)
                .build();
    }

    public static RoomStateMessage phaseChange(GamePhase phase) {
        return RoomStateMessage.builder()
                .type("PHASE_CHANGE")
                .phase(phase)
                .build();
    }

    public static RoomStateMessage contentSelected(Long contentId) {
        return RoomStateMessage.builder()
                .type("CONTENT_SELECTED")
                .contentId(contentId)
                .build();
    }

    public static RoomStateMessage gameStart(Long contentId, GamePhase phase) {
        return RoomStateMessage.builder()
                .type("GAME_START")
                .contentId(contentId)
                .phase(phase)
                .build();
    }

    public static RoomStateMessage roundStart(GamePhase phase, Integer round, Long serverTime, List<MemberSegmentInfo> segments) {
        return RoomStateMessage.builder()
                .type("ROUND_START")
                .phase(phase)
                .round(round)
                .serverTime(serverTime)
                .segments(segments)
                .build();
    }

    public static RoomStateMessage recordingsComplete(GamePhase phase, Integer round) {
        return RoomStateMessage.builder()
                .type("RECORDINGS_COMPLETE")
                .phase(phase)
                .round(round)
                .build();
    }
}
