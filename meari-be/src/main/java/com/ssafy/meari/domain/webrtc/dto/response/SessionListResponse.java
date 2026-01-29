package com.ssafy.meari.domain.webrtc.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "활성 세션 목록 항목")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SessionListResponse {

    @Schema(description = "세션 ID", example = "room_1")
    private String sessionId;

    public static SessionListResponse of(String sessionId) {
        return SessionListResponse.builder()
                .sessionId(sessionId)
                .build();
    }
}
