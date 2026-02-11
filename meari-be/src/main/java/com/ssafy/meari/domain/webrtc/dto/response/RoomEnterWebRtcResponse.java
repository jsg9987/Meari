package com.ssafy.meari.domain.webrtc.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "방 입장 응답 (WebRTC 토큰 포함)")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoomEnterWebRtcResponse {

    @Schema(description = "방 ID", example = "1")
    private Long roomId;

    @Schema(description = "OpenVidu 세션 ID", example = "room_1")
    private String sessionId;

    @Schema(description = "WebRTC 연결 토큰", example = "wss://localhost:4443?sessionId=room_1&token=tok_ABC123")
    private String token;

    @Schema(description = "연결 ID", example = "con_XYZ789abc")
    private String connectionId;

    @Schema(description = "현재 참여자 수", example = "1")
    private Integer currentPeople;

    @Schema(description = "최대 정원", example = "2")
    private Integer maxPeople;

    public static RoomEnterWebRtcResponse of(Long roomId, String sessionId, String token,
            String connectionId, Integer currentPeople, Integer maxPeople) {
        return RoomEnterWebRtcResponse.builder()
                .roomId(roomId)
                .sessionId(sessionId)
                .token(token)
                .connectionId(connectionId)
                .currentPeople(currentPeople)
                .maxPeople(maxPeople)
                .build();
    }
}
