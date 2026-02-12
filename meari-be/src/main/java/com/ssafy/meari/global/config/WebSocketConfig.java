package com.ssafy.meari.global.config;

import com.ssafy.meari.domain.room.interceptor.RoomSessionMappingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final RoomSessionMappingInterceptor roomSessionMappingInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독할 목적지 prefix
        // SimpleBroker 사용 (인메모리 메시지 브로커)
        // /topic: 1:N 브로드캐스트
        // /queue: 1:1 메시지
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 메시지를 보낼 때 사용할 prefix
        registry.setApplicationDestinationPrefixes("/app");

        log.info("SimpleBroker 설정 완료 - /topic, /queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // SockJS 없이 순수 WebSocket 연결
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        log.info("WebSocket STOMP 엔드포인트 등록: /ws");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT 인증 인터셉터 등록 (먼저 실행)
        registration.interceptors(jwtChannelInterceptor);
        // sessionId 매핑 인터셉터 등록 (JWT 인증 후 실행)
        registration.interceptors(roomSessionMappingInterceptor);
        log.info("WebSocket 인터셉터 등록 완료: JWT 인증, sessionId 매핑");
    }
}
