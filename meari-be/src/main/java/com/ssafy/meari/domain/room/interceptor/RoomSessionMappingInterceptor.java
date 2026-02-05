package com.ssafy.meari.domain.room.interceptor;

import com.ssafy.meari.domain.room.service.RoomSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket SUBSCRIBE 시점에 sessionId 매핑을 자동으로 저장하는 Interceptor
 * 클라이언트가 /topic/room/{roomId}/state를 구독할 때 sessionId → memberId, roomId 매핑 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSessionMappingInterceptor implements ChannelInterceptor {

    private final RoomSessionService roomSessionService;
    private static final Pattern ROOM_PATTERN = Pattern.compile("/topic/room/(\\d+)/");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        // SUBSCRIBE 메시지만 처리
        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();

            // /topic/room/{roomId}/... 구독 시
            if (destination != null && destination.startsWith("/topic/room/")) {
                handleRoomSubscribe(accessor, destination);
            }
        }

        return message;
    }

    private void handleRoomSubscribe(StompHeaderAccessor accessor, String destination) {
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            log.warn("sessionId가 null입니다: destination={}", destination);
            return;
        }

        // 이미 저장되어 있으면 스킵 (중복 방지)
        if (roomSessionService.hasSessionMapping(sessionId)) {
            log.debug("sessionId 매핑 이미 존재, 스킵: sessionId={}", sessionId);
            return;
        }

        try {
            // destination에서 roomId 추출
            Long roomId = extractRoomId(destination);
            if (roomId == null) {
                log.warn("roomId 추출 실패: destination={}", destination);
                return;
            }

            // Principal에서 memberId 추출
            Principal principal = accessor.getUser();
            if (principal == null) {
                log.warn("Principal이 null입니다. WebSocket 인증이 설정되지 않았을 수 있습니다: sessionId={}", sessionId);
                return;
            }

            // Principal.getName()이 memberId라고 가정
            Long memberId;
            try {
                memberId = Long.parseLong(principal.getName());
            } catch (NumberFormatException e) {
                log.warn("Principal.getName()을 memberId로 파싱 실패: principal={}", principal.getName(), e);
                return;
            }

            // sessionId 매핑 저장
            roomSessionService.setSessionMember(sessionId, memberId, roomId);

            log.info("✅ [Interceptor] SUBSCRIBE 시 sessionId 매핑 저장: sessionId={}, roomId={}, memberId={}",
                    sessionId, roomId, memberId);

        } catch (Exception e) {
            log.error("sessionId 매핑 저장 중 오류 발생: sessionId={}", sessionId, e);
        }
    }

    private Long extractRoomId(String destination) {
        // /topic/room/123/state → 123 추출
        Matcher matcher = ROOM_PATTERN.matcher(destination);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }
}
