package com.ssafy.meari.domain.room.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

// Redis에 저장되는 채팅 메세지 객체
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash(value = "chat", timeToLive = 43200)
@Schema(description = "실시간 채팅 메시지 (Redis)")
public class Chat implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Schema(description = "메시지 ID")
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Indexed
    @Schema(description = "방 ID")
    private Long roomId;

    @Schema(description = "송신자 ID")
    private Long senderId;

    @Schema(description = "송신자 닉네임")
    private String nickname;

    @Schema(description = "메시지 내용")
    private String message;

    @Schema(description = "메시지 전송 시각")
    private LocalDateTime timestamp;
}
