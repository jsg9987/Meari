package com.ssafy.meari.global.config;

import com.ssafy.meari.domain.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 연결/해제 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RoomService roomService;

    /**
     * WebSocket 연결 해제 시 자동 퇴장 처리
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // 인증 정보에서 사용자 정보 추출
        Authentication authentication = (Authentication) accessor.getUser();

        if (authentication != null) {
            // UserDetailsImpl에서 memberId 추출
            com.ssafy.meari.global.auth.UserDetailsImpl userDetails =
                (com.ssafy.meari.global.auth.UserDetailsImpl) authentication.getPrincipal();
            Long memberId = userDetails.getMember().getMemberId();

            log.info("WebSocket 연결 해제 감지: memberId={}, sessionId={}",
                    memberId, accessor.getSessionId());

            // TODO: 현재 참여 중인 방 ID를 찾아서 자동 퇴장 처리
            // Redis에 member:session 매핑을 저장하거나,
            // session attributes에 roomId를 저장하여 추적 가능

            // 임시: Grace Period 처리는 추후 구현
            // roomService.handleDisconnect(roomId, memberId);
        }
    }
}
