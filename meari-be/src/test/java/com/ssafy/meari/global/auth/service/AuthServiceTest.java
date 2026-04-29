package com.ssafy.meari.global.auth.service;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.domain.member.mapper.MemberMapper;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.auth.jwt.JwtUtil;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AuthService 단위 테스트
 * - JWT 검증/Redis/DB 모두 Mockito로 격리
 * - 검증 대상: refreshAccessToken 분기 로직, logout 흐름
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private Claims claims;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "user@example.com";
    private static final String REFRESH_TOKEN = "valid.refresh.token";
    private static final String NEW_ACCESS_TOKEN = "new.access.token";

    private Member buildMember() {
        return Member.builder()
            .email(EMAIL)
            .password("encoded")
            .nickname("nickname")
            .nativeLanguage(NativeLanguage.KR)
            .build();
    }

    @Nested
    @DisplayName("refreshAccessToken")
    class RefreshAccessToken {

        @Test
        @DisplayName("성공 - 유효한 refresh token으로 새 access token 발급")
        void success() {
            // given
            given(jwtUtil.validateToken(REFRESH_TOKEN)).willReturn(claims);
            given(claims.getSubject()).willReturn(EMAIL);
            given(refreshTokenService.getRefreshToken(EMAIL)).willReturn(REFRESH_TOKEN);
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(buildMember()));
            given(jwtUtil.generateAccessToken(any(UserDetailsImpl.class))).willReturn(NEW_ACCESS_TOKEN);

            // when
            String result = authService.refreshAccessToken(REFRESH_TOKEN);

            // then
            assertThat(result).isEqualTo(NEW_ACCESS_TOKEN);
            verify(jwtUtil).generateAccessToken(any(UserDetailsImpl.class));
        }

        @Test
        @DisplayName("실패 - Redis에 저장된 refresh token이 없으면 REFRESH_TOKEN_NOT_FOUND")
        void fail_storedTokenNull() {
            // given
            given(jwtUtil.validateToken(REFRESH_TOKEN)).willReturn(claims);
            given(claims.getSubject()).willReturn(EMAIL);
            given(refreshTokenService.getRefreshToken(EMAIL)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - Redis 저장 토큰과 다르면 INVALID_REFRESH_TOKEN")
        void fail_storedTokenMismatch() {
            // given
            given(jwtUtil.validateToken(REFRESH_TOKEN)).willReturn(claims);
            given(claims.getSubject()).willReturn(EMAIL);
            given(refreshTokenService.getRefreshToken(EMAIL)).willReturn("different.token");

            // when & then
            assertThatThrownBy(() -> authService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("실패 - DB에 회원이 없으면 NOT_FOUND_MEMBER")
        void fail_memberNotFound() {
            // given
            given(jwtUtil.validateToken(REFRESH_TOKEN)).willReturn(claims);
            given(claims.getSubject()).willReturn(EMAIL);
            given(refreshTokenService.getRefreshToken(EMAIL)).willReturn(REFRESH_TOKEN);
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND_MEMBER);
        }

        @Test
        @DisplayName("실패 - JWT 검증 단계에서 예외(만료/변조 등) 발생 시 INVALID_REFRESH_TOKEN으로 변환")
        void fail_jwtValidationThrowsGenericException() {
            // given
            given(jwtUtil.validateToken(REFRESH_TOKEN))
                .willThrow(new RuntimeException("expired"));

            // when & then
            assertThatThrownBy(() -> authService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("성공 - access token 블랙리스트 등록 + Redis refresh token 삭제")
        void success() {
            // given
            String accessToken = "access.token";

            // when
            authService.logout(EMAIL, accessToken);

            // then
            verify(tokenBlacklistService).addToBlacklist(accessToken);
            verify(refreshTokenService).deleteRefreshToken(EMAIL);
        }
    }
}
