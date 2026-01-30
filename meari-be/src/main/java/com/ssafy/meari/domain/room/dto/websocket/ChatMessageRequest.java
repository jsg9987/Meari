package com.ssafy.meari.domain.room.dto.websocket;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Generated;

import java.time.LocalDateTime;

// 채팅 전송 요청 dto
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "실시간 채팅 메시지"
)
public class ChatMessageRequest {
    @Schema(
            description = "송신자 ID"
    )
    private Long senderId;
    @Schema(
            description = "송신자 닉네임"
    )
    private String nickname;
    @Schema(
            description = "메시지 내용"
    )
    private @Size(
            max = 1000,
            message = "메시지는 1000자 이내여야 합니다"
    ) String message;
    @Schema(
            description = "메시지 전송 시각"
    )
    private LocalDateTime timestamp;

    @Generated
    public static ChatMessageBuilder builder() {
        return new ChatMessageBuilder();
    }

    @Generated
    public Long getSenderId() {
        return this.senderId;
    }

    @Generated
    public String getNickname() {
        return this.nickname;
    }

    @Generated
    public String getMessage() {
        return this.message;
    }

    @Generated
    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    @Generated
    public ChatMessageRequest() {
    }

    @Generated
    public ChatMessageRequest(final Long senderId, final String nickname, final String message, final LocalDateTime timestamp) {
        this.senderId = senderId;
        this.nickname = nickname;
        this.message = message;
        this.timestamp = timestamp;
    }

    @Generated
    public static class ChatMessageBuilder {
        @Generated
        private Long senderId;
        @Generated
        private String nickname;
        @Generated
        private String message;
        @Generated
        private LocalDateTime timestamp;

        @Generated
        ChatMessageBuilder() {
        }

        @Generated
        public ChatMessageBuilder senderId(final Long senderId) {
            this.senderId = senderId;
            return this;
        }

        @Generated
        public ChatMessageBuilder nickname(final String nickname) {
            this.nickname = nickname;
            return this;
        }

        @Generated
        public ChatMessageBuilder message(final String message) {
            this.message = message;
            return this;
        }

        @Generated
        public ChatMessageBuilder timestamp(final LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @Generated
        public ChatMessageRequest build() {
            return new ChatMessageRequest(this.senderId, this.nickname, this.message, this.timestamp);
        }

        @Generated
        public String toString() {
            Long var10000 = this.senderId;
            return "ChatMessage.ChatMessageBuilder(senderId=" + var10000 + ", nickname=" + this.nickname + ", message=" + this.message + ", timestamp=" + String.valueOf(this.timestamp) + ")";
        }
    }
}
