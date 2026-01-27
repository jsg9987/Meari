package com.ssafy.meari.domain.room.controller;

import com.ssafy.meari.domain.room.dto.websocket.*;
import com.ssafy.meari.domain.room.entity.Chat;
import com.ssafy.meari.domain.room.repository.ChatRepository;
import com.ssafy.meari.domain.room.service.RoomSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Room WebSocket 메시지 핸들러
 *
 * 클라이언트 → 서버: /app/room/{roomId}/...
 * 서버 → 클라이언트: /topic/room/{roomId}/...
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomSessionService roomSessionService;
    private final ChatRepository chatRepository;

    private static final String TOPIC_STATE = "/topic/room/%d/state";
    private static final String TOPIC_CHAT = "/topic/room/%d/chat";

    /**
     * 준비 상태 토글
     * 클라이언트: /app/room/{roomId}/ready
     */
    @MessageMapping("/room/{roomId}/ready")
    public void toggleReady(
            @DestinationVariable Long roomId,
            @Payload ReadyMessage message
    ) {
        log.info("준비 상태 변경 요청: roomId={}, memberId={}", roomId, message.getMemberId());

        boolean currentReady = roomSessionService.isReady(roomId, message.getMemberId());
        boolean newReady = !currentReady;
        roomSessionService.setReady(roomId, message.getMemberId(), newReady);

        // 전체 참여자에게 브로드캐스트
        RoomStateMessage stateMessage = RoomStateMessage.ready(message.getMemberId(), newReady);
        broadcast(roomId, TOPIC_STATE, stateMessage);
    }

    /**
     * 역할 선점
     * 클라이언트: /app/room/{roomId}/role
     */
    @MessageMapping("/room/{roomId}/role")
    public void selectRole(
            @DestinationVariable Long roomId,
            @Payload RoleSelectMessage message
    ) {
        log.info("역할 선점 요청: roomId={}, memberId={}, roleId={}",
                roomId, message.getMemberId(), message.getRoleId());

        boolean success = roomSessionService.tryAssignRole(roomId, message.getRoleId(), message.getMemberId());

        if (success) {
            // 선점 성공 시 전체 브로드캐스트
            RoomStateMessage stateMessage = RoomStateMessage.roleAssigned(
                    message.getMemberId(), message.getRoleId());
            broadcast(roomId, TOPIC_STATE, stateMessage);
        } else {
            // 실패 시 요청자에게만 알림 (추후 개인 메시지 구현 가능)
            log.warn("역할 선점 실패: roomId={}, roleId={} (이미 선점됨)", roomId, message.getRoleId());
        }
    }

    /**
     * 역할 해제
     * 클라이언트: /app/room/{roomId}/role/release
     */
    @MessageMapping("/room/{roomId}/role/release")
    public void releaseRole(
            @DestinationVariable Long roomId,
            @Payload RoleReleaseMessage message
    ) {
        log.info("역할 해제 요청: roomId={}, memberId={}", roomId, message.getMemberId());

        Long roleId = roomSessionService.getMemberRole(roomId, message.getMemberId());
        if (roleId != null) {
            roomSessionService.releaseRole(roomId, roleId);

            RoomStateMessage stateMessage = RoomStateMessage.roleReleased(message.getMemberId(), roleId);
            broadcast(roomId, TOPIC_STATE, stateMessage);
        }
    }

    /**
     * 채팅 메시지
     * 클라이언트: /app/room/{roomId}/chat
     */
    @MessageMapping("/room/{roomId}/chat")
    public void chat(
            @DestinationVariable Long roomId,
            @Payload ChatMessage message
    ) {
        log.info("[Chat] 채팅 메시지 서버수신: roomId={}, senderId={}, nickname={}, message={}",
                roomId, message.getSenderId(), message.getNickname(), message.getMessage());

        // 타임스탬프 자동 설정
        message.setTimestampNow();

        // Redis에 채팅 메시지 저장
        Chat chat = Chat.builder()
                .roomId(roomId)
                .senderId(message.getSenderId())
                .nickname(message.getNickname())
                .message(message.getMessage())
                .timestamp(message.getTimestamp())
                .build();
        chatRepository.save(chat);

        // 전체 참여자에게 브로드캐스트
        broadcast(roomId, TOPIC_CHAT, message);

        log.debug("[Chat] 채팅 메시지 브로드캐스트 완료: roomId={}", roomId);
    }

    /**
     * 방 상태 변경 브로드캐스트 (외부에서 호출용)
     */
    public void broadcastStateChange(Long roomId, RoomStateMessage message) {
        broadcast(roomId, TOPIC_STATE, message);
    }

    /**
     * 메시지 브로드캐스트
     */
    private void broadcast(Long roomId, String topicPattern, Object message) {
        String destination = String.format(topicPattern, roomId);
        messagingTemplate.convertAndSend(destination, message);
        log.debug("브로드캐스트: destination={}", destination);
    }
}
