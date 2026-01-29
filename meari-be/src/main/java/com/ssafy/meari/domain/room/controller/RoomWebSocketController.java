package com.ssafy.meari.domain.room.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.ssafy.meari.domain.room.dto.websocket.ChatMessage;
import com.ssafy.meari.domain.room.dto.websocket.ReadyMessage;
import com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage;
import com.ssafy.meari.domain.room.dto.websocket.RoleReleaseMessage;
import com.ssafy.meari.domain.room.dto.websocket.RoleSelectMessage;
import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;
import com.ssafy.meari.domain.room.entity.Chat;
import com.ssafy.meari.domain.room.repository.ChatRepository;
import com.ssafy.meari.domain.room.service.RoomService;
import com.ssafy.meari.domain.room.service.RoomSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final RoomService roomService;
    private final ChatRepository chatRepository;

    private static final String TOPIC_STATE = "/topic/room.%d.state";
    private static final String TOPIC_CHAT = "/topic/room.%d.chat";
    private static final int MAX_CHAT_COUNT = 100;

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
        clearDisconnectedIfNeeded(roomId, message.getMemberId());

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
        clearDisconnectedIfNeeded(roomId, message.getMemberId());

        // 역할이 이미 확정되었는지 확인
        if (roomSessionService.isRolesConfirmed(roomId)) {
            log.warn("역할 선택 실패: roomId={}, 이미 역할이 확정됨", roomId);
            // TODO: 개인 에러 메시지 전송 (추후 /queue/errors 구현)
            return;
        }

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
        clearDisconnectedIfNeeded(roomId, message.getMemberId());

        // 역할이 이미 확정되었는지 확인
        if (roomSessionService.isRolesConfirmed(roomId)) {
            log.warn("역할 해제 실패: roomId={}, 이미 역할이 확정됨", roomId);
            // TODO: 개인 에러 메시지 전송 (추후 /queue/errors 구현)
            return;
        }

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
        log.info("채팅 메시지 서버수신: roomId={}, senderId={}, nickname={}, message={}",
                roomId, message.getSenderId(), message.getNickname(), message.getMessage());

        try {
            // Redis에 채팅 메시지 저장
            Chat chat = Chat.builder()
                    .roomId(roomId)
                    .senderId(message.getSenderId())
                    .nickname(message.getNickname())
                    .message(message.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            chatRepository.save(chat);

            // 100개 초과 시 오래된 메시지 삭제
            trimOldMessages(roomId);

            // 전체 참여자에게 브로드캐스트
            broadcast(roomId, TOPIC_CHAT, message);

            log.debug("채팅 메시지 브로드캐스트 완료: roomId={}", roomId);

        } catch (Exception e) {
            log.error("채팅 메시지 저장/브로드캐스트 실패: roomId={}, senderId={}", roomId, message.getSenderId(), e);
            // 추후 채팅 전송 실패 시 에러 메시지를 발신자에게만 전송하는 기능 추가를 고려할 수 있다.
        }
    }

    /**
     * 문장별 녹음 완료
     * 클라이언트: /app/room/{roomId}/recording/complete
     */
    @MessageMapping("/room/{roomId}/recording/complete")
    public void recordingComplete(
            @DestinationVariable Long roomId,
            @Payload RecordingCompleteMessage message
    ) {
        log.info("녹음 완료 메시지 수신: roomId={}, memberId={}, sentenceId={}",
                roomId, message.getMemberId(), message.getSentenceId());
        clearDisconnectedIfNeeded(roomId, message.getMemberId());

        roomService.recordingComplete(roomId, message);
    }

    /**
     * 재연결 시 disconnected 마킹 해제
     * WebSocket 메시지 핸들러에서 호출하여 Grace Period 내 재연결 감지
     */
    private void clearDisconnectedIfNeeded(Long roomId, Long memberId) {
        if (roomSessionService.isDisconnected(roomId, memberId)) {
            roomSessionService.clearDisconnected(roomId, memberId);
            log.info("재연결 감지, disconnected 마킹 해제: roomId={}, memberId={}", roomId, memberId);
        }
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

    /**
     * 방의 채팅 메시지가 MAX_CHAT_COUNT를 초과하면 오래된 순으로 삭제
     */
    private void trimOldMessages(Long roomId) {
        List<Chat> chats = chatRepository.findByRoomIdOrderByTimestampAsc(roomId);

        if (chats.size() > MAX_CHAT_COUNT) {
            int deleteCount = chats.size() - MAX_CHAT_COUNT;
            List<Chat> oldChats = chats.subList(0, deleteCount);
            chatRepository.deleteAll(oldChats);
            log.debug("오래된 채팅 메시지 삭제: roomId={}, 삭제 개수={}", roomId, deleteCount);
        }
    }
}
