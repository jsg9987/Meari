package com.ssafy.meari.domain.webrtc.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "OpenVidu 연결(토큰) 생성 요청")
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenViduConnectionRequest {

    @Schema(description = "사용자 ID", example = "1")
    private Long memberId;

    @Schema(description = "사용자 닉네임", example = "김철수")
    private String nickname;

    @Schema(description = "역할 ID", example = "2")
    private Long roleId;
}
