package com.ssafy.meari.global.auth.jwt;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.auth.service.TokenBlacklistService;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 단위 테스트
 * - HttpServletRequest/Response/FilterChain은 Mock
 * - JwtUtil/TokenBlacklistService/UserDetailsService Mock으로 분기 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 단위 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserDetailsImpl userDetails(String email) {
        Member member = Member.builder()
            .email(email)
            .password("encoded")
            .nickname("nick")
            .nativeLanguage(NativeLanguage.KR)
            .build();
        return new UserDetailsImpl(member);
    }

    @Nested
    @DisplayName("스킵 경로 - JWT 검증 우회")
    class SkipPaths {

        @Test
        @DisplayName("/api/v1/auth/login 은 스킵하고 다음 필터로 진행")
        void skipLogin() throws Exception {
            given(request.getRequestURI()).willReturn("/api/v1/auth/login");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtUtil, tokenBlacklistService, userDetailsService);
        }

        @Test
        @DisplayName("/api/v1/auth/signup 도 스킵")
        void skipSignup() throws Exception {
            given(request.getRequestURI()).willReturn("/api/v1/auth/signup");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("/swagger-ui 하위 경로도 스킵")
        void skipSwagger() throws Exception {
            given(request.getRequestURI()).willReturn("/swagger-ui/index.html");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtUtil);
        }
    }

    @Nested
    @DisplayName("Authorization 헤더 검증")
    class HeaderValidation {

        @Test
        @DisplayName("실패 - Authorization 헤더 없으면 INVALID_HEADER_ERROR")
        void fail_noAuthHeader() {
            given(request.getRequestURI()).willReturn("/api/v1/members/me");
            given(request.getHeader("Authorization")).willReturn(null);

            assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_HEADER_ERROR);
        }

        @Test
        @DisplayName("실패 - 'Bearer ' prefix 없으면 INVALID_HEADER_ERROR")
        void fail_noBearerPrefix() {
            given(request.getRequestURI()).willReturn("/api/v1/members/me");
            given(request.getHeader("Authorization")).willReturn("some.raw.token");

            assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_HEADER_ERROR);
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    class TokenValidation {

        @Test
        @DisplayName("성공 - 유효 토큰이면 SecurityContext 설정 후 다음 필터로 진행")
        void success_validToken() throws Exception {
            String email = "user@example.com";
            given(request.getRequestURI()).willReturn("/api/v1/members/me");
            given(request.getHeader("Authorization")).willReturn("Bearer valid.token");
            given(jwtUtil.validateToken("valid.token")).willReturn(claims);
            given(tokenBlacklistService.isBlacklisted("valid.token")).willReturn(false);
            given(claims.getSubject()).willReturn(email);
            given(userDetailsService.loadUserByUsername(email)).willReturn(userDetails(email));

            filter.doFilter(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(((UserDetailsImpl) auth.getPrincipal()).getUsername()).isEqualTo(email);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("실패 - 블랙리스트 토큰이면 TOKEN_BLACKLISTED 던지고 다음 필터 진행 안 함")
        void fail_blacklistedToken() {
            given(request.getRequestURI()).willReturn("/api/v1/members/me");
            given(request.getHeader("Authorization")).willReturn("Bearer blacklisted.token");
            given(jwtUtil.validateToken("blacklisted.token")).willReturn(claims);
            given(tokenBlacklistService.isBlacklisted("blacklisted.token")).willReturn(true);

            assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_BLACKLISTED);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
