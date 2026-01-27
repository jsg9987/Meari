package com.ssafy.meari.global.auth.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import com.ssafy.meari.global.auth.service.TokenBlacklistService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // JWT 검증 스킵할 경로들
        if ("/api/v1/auth/login".equals(requestURI) ||
            "/api/v1/auth/signup".equals(requestURI) ||
            "/api/v1/auth/email/check".equals(requestURI) ||
            "/api/v1/auth/nickname/check".equals(requestURI) ||
            "/api/v1/auth/refresh".equals(requestURI) ||
            requestURI.startsWith("/swagger-ui") ||    // swagger-ui 관련 모든 리소스
            requestURI.startsWith("/v3/api-docs") ||   // OpenAPI3 스펙 경로
            requestURI.startsWith("/api-docs")         // 기존 api-docs 경로
        ) {
            log.debug("JwtAuthentication 스킵");
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        // 1. "Authorization" 헤더가 없거나 "Bearer "로 시작하지 않으면 skip
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.INVALID_HEADER_ERROR);
        }

        // 2. "Bearer " 접두사 제거
        String accessToken = authorizationHeader.substring(7);

        // 3. 토큰 유효성 검증
        if (StringUtils.hasText(accessToken)) {
            // 3-1. JWT 서명 및 만료 검증
            Claims claims = jwtUtil.validateToken(accessToken);

            // 3-2. 블랙리스트 확인 (로그아웃된 토큰 체크)
            if (tokenBlacklistService.isBlacklisted(accessToken)) {
                throw new BusinessException(ErrorCode.TOKEN_BLACKLISTED);
            }

            // 3-3. 사용자 인증 정보 설정
            String email = claims.getSubject();
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
