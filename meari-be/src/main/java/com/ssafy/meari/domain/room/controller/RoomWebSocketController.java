package com.ssafy.meari.domain.room.controller;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.ssafy.meari.domain.room.dto.websocket.ChatMessage;
import com.ssafy.meari.domain.room.dto.websocket.ReadyMessage;
import com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage;
import com.ssafy.meari.domain.room.dto.websocket.RoleReleaseMessage;
import com.ssafy.meari.domain.room.dto.websocket.RoleSelectMessage;
import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;
import com.ssafy.meari.domain.room.dto.websocket.WatchingCompleteMessage;
import com.ssafy.meari.domain.room.service.ChatService;
import com.ssafy.meari.domain.room.service.RoomCommandService;
import com.ssafy.meari.domain.room.service.RoomPhaseService;
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
    private final RoomCommandService roomCommandService;
    private final RoomPhaseService roomPhaseService;
    private final ChatService chatService;

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
        log.info("채팅 메시지: roomId={}, memberId={}, message={}",
                roomId, message.getSenderId(), message.getMessage());

        clearDisconnectedIfNeeded(roomId, message.getSenderId());

        try {
            // 채팅 메시지 저장 (Redis)
            chatService.saveChat(roomId, message);

            // 전체 참여자에게 브로드캐스트
            broadcast(roomId, TOPIC_CHAT, message);

            log.debug("채팅 메시지 브로드캐스트 완료: roomId={}", roomId);

        } catch (Exception e) {
            log.error("채팅 메시지 저장/브로드캐스트 실패: roomId={}, senderId={}", roomId, message.getSenderId(), e);
        }
    }

    /**
     * 영상 시청 완료 (참여자 개인)
     * 클라이언트: /app/room/{roomId}/watching/complete
     * 4명 모두 완료 시 서버가 PHASE_CHANGE(ROLE_PICK) 브로드캐스트
     */
    @MessageMapping("/room/{roomId}/watching/complete")
    public void watchingComplete(
            @DestinationVariable Long roomId,
            @Payload WatchingCompleteMessage message
    ) {
        log.info("영상 시청 완료 메시지 수신: roomId={}, memberId={}", roomId, message.getMemberId());

        clearDisconnectedIfNeeded(roomId, message.getMemberId());

        roomPhaseService.watchingComplete(roomId, message.getMemberId());
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

        roomPhaseService.recordingComplete(roomId, message);
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
     * WebSocket 연결 끊김 이벤트 처리
     * 클라이언트가 연결을 끊으면 (강제 종료, 네트워크 끊김 등) 즉시 처리
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId == null) {
            log.warn("WebSocket 연결 끊김: sessionId 조회 실패");
            return;
        }

        log.info("WebSocket 연결 끊김 감지: sessionId={}", sessionId);

        try {
            // Redis에서 sessionId 매핑 조회
            Long memberId = roomSessionService.getSessionMemberId(sessionId);
            Long roomId = roomSessionService.getSessionRoomId(sessionId);

            if (memberId != null && roomId != null) {
                // Redis 즉시 삭제 (실시간 상태 반영)
                roomSessionService.removeMember(roomId, memberId);
                roomSessionService.clearMemberRoom(memberId);
                roomSessionService.clearSessionMember(sessionId);

                log.info("비정상 종료: Redis 제거 완료, roomId={}, memberId={}", roomId, memberId);

                // Service 호출해서 DB 정리 및 방장 위임/방 종료 처리
                roomCommandService.handleAbnormalDisconnect(roomId, memberId);
            } else {
                log.debug("WebSocket 연결 끊김: sessionId 매핑 정보 없음 (아직 메시지 미전송 상태)");
            }
        } catch (Exception e) {
            log.error("WebSocket 연결 끊김 처리 중 오류: sessionId={}", sessionId, e);
        }
    }
}
