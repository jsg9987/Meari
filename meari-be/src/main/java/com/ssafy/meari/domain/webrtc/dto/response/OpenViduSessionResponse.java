package com.ssafy.meari.domain.webrtc.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "OpenVidu 세션 생성 응답")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenViduSessionResponse {

    @Schema(description = "세션 ID", example = "ses_ABC123xyz")
    private String sessionId;

    public static OpenViduSessionResponse of(String sessionId) {
        return OpenViduSessionResponse.builder()
                .sessionId(sessionId)
                .build();
    }
}
