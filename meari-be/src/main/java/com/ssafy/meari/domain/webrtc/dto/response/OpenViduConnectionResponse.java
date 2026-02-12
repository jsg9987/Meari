package com.ssafy.meari.domain.webrtc.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "OpenVidu 연결(토큰) 생성 응답")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenViduConnectionResponse {

    @Schema(description = "세션 ID", example = "ses_ABC123xyz")
    private String sessionId;

    @Schema(description = "연결 토큰 (WebSocket URL)", example = "wss://your-openvidu-server:443?sessionId=ses_ABC123xyz&token=tok_...")
    private String token;

    @Schema(description = "연결 ID", example = "con_XYZ789abc")
    private String connectionId;

    public static OpenViduConnectionResponse of(String sessionId, String token, String connectionId) {
        return OpenViduConnectionResponse.builder()
                .sessionId(sessionId)
                .token(token)
                .connectionId(connectionId)
                .build();
    }
}
