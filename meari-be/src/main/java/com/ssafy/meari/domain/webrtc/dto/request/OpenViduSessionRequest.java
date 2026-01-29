package com.ssafy.meari.domain.webrtc.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "OpenVidu 세션 생성 요청")
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenViduSessionRequest {

    @Schema(description = "커스텀 세션 ID", example = "room_123")
    private String customSessionId;

    @Schema(description = "연결할 Room ID", example = "1")
    private Long roomId;
}
