package com.ssafy.meari.domain.room.dto.websocket;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

/**
 * 방 상태 변경 브로드캐스트 메시지
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoomStateMessage {

    private String type;      // MEMBER_JOIN, MEMBER_LEAVE, READY, ROLE_ASSIGNED, ROLE_RELEASED, PHASE_CHANGE
    private Long memberId;
    private Boolean ready;
    private Long roleId;
    private String phase;
    private String nickname;
    private Long newOwnerId;  // 방장 변경 시

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

    public static RoomStateMessage phaseChange(String phase) {
        return RoomStateMessage.builder()
                .type("PHASE_CHANGE")
                .phase(phase)
                .build();
    }
}
