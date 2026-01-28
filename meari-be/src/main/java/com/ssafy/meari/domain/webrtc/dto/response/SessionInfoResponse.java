package com.ssafy.meari.domain.webrtc.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "OpenVidu 세션 정보 응답")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SessionInfoResponse {

    @Schema(description = "세션 ID", example = "ses_ABC123xyz")
    private String sessionId;

    @Schema(description = "세션 생성 시간 (Unix timestamp)", example = "1706234567890")
    private Long createdAt;

    @Schema(description = "연결된 참여자 목록")
    private List<ConnectionInfo> connections;

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ConnectionInfo {

        @Schema(description = "연결 ID", example = "con_XYZ789abc")
        private String connectionId;

        @Schema(description = "연결 생성 시간 (Unix timestamp)", example = "1706234570000")
        private Long createdAt;

        @Schema(description = "클라이언트 플랫폼 정보", example = "Chrome 120.0.0")
        private String platform;

        @Schema(description = "클라이언트 데이터 (JSON)", example = "{\"memberId\":1,\"nickname\":\"김철수\",\"roleId\":2}")
        private String clientData;

        public static ConnectionInfo of(String connectionId, Long createdAt, String platform, String clientData) {
            return ConnectionInfo.builder()
                    .connectionId(connectionId)
                    .createdAt(createdAt)
                    .platform(platform)
                    .clientData(clientData)
                    .build();
        }
    }

    public static SessionInfoResponse of(String sessionId, Long createdAt, List<ConnectionInfo> connections) {
        return SessionInfoResponse.builder()
                .sessionId(sessionId)
                .createdAt(createdAt)
                .connections(connections)
                .build();
    }
}
