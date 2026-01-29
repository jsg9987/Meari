package com.ssafy.meari.domain.room.dto.websocket;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "실시간 채팅 메시지")
public class ChatMessage {

    @Schema(description = "송신자 ID")
    private Long senderId;

    @Schema(description = "송신자 닉네임")
    private String nickname;

    @Size(max = 1000, message = "메시지는 1000자 이내여야 합니다")
    @Schema(description = "메시지 내용")
    private String message;

    @Schema(description = "메시지 전송 시각")
    private LocalDateTime timestamp;

}
