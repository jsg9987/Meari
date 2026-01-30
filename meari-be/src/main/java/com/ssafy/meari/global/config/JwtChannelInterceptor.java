package com.ssafy.meari.global.config;

import com.ssafy.meari.global.auth.UserDetailsServiceImpl;
import com.ssafy.meari.global.auth.jwt.JwtUtil;
import com.ssafy.meari.global.auth.service.TokenBlacklistService;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * WebSocket STOMP 연결 시 JWT 인증을 처리하는 인터셉터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("WebSocket CONNECT 요청 - JWT 인증 시작");

            // Authorization 헤더에서 토큰 추출
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

            if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
                log.error("WebSocket 인증 실패: Authorization 헤더 없음 또는 형식 오류");
                throw new BusinessException(ErrorCode.INVALID_HEADER_ERROR);
            }

            String accessToken = authorizationHeader.substring(7);

            try {
                // JWT 검증
                Claims claims = jwtUtil.validateToken(accessToken);

                // 블랙리스트 확인
                if (tokenBlacklistService.isBlacklisted(accessToken)) {
                    log.error("WebSocket 인증 실패: 블랙리스트 토큰");
                    throw new BusinessException(ErrorCode.TOKEN_BLACKLISTED);
                }

                // 사용자 정보 로드
                String email = claims.getSubject();
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 인증 정보 설정
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                accessor.setUser(authentication);
                log.info("WebSocket 인증 성공: email={}", email);

            } catch (BusinessException e) {
                log.error("WebSocket JWT 인증 실패: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("WebSocket JWT 검증 중 예외 발생", e);
                throw new BusinessException(ErrorCode.INVALID_TOKEN_ERROR);
            }
        }

        return message;
    }
}
