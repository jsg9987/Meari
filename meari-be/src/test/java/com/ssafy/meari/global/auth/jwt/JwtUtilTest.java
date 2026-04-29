package com.ssafy.meari.global.auth.jwt;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtUtil 단위 테스트
 * - Spring Context 없이 ReflectionTestUtils로 @Value 필드 주입
 * - 라운드트립(생성→검증) + 위변조/만료 실패 케이스
 */
@DisplayName("JwtUtil 단위 테스트")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "test-secret-key-of-sufficient-length-for-hs256-signing";
    private static final long ACCESS_EXPIRE_MS = 60_000L;        // 1분
    private static final long REFRESH_EXPIRE_MS = 600_000L;      // 10분

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirePeriodMs", ACCESS_EXPIRE_MS);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpirePeriodMs", REFRESH_EXPIRE_MS);
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
    @DisplayName("generateAccessToken / validateToken (라운드트립)")
    class RoundTrip {

        @Test
        @DisplayName("성공 - access token 생성 후 검증 시 subject=email, type=access")
        void accessToken_roundTrip() {
            // given
            String email = "user@example.com";

            // when
            String token = jwtUtil.generateAccessToken(userDetails(email));
            Claims claims = jwtUtil.validateToken(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(email);
            assertThat(claims.get("type", String.class)).isEqualTo("access");
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }

        @Test
        @DisplayName("성공 - refresh token type=refresh로 발급됨")
        void refreshToken_roundTrip() {
            String token = jwtUtil.generateRefreshToken(userDetails("user@example.com"));
            Claims claims = jwtUtil.validateToken(token);

            assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        }
    }

    @Nested
    @DisplayName("validateToken 실패 케이스")
    class ValidateFailure {

        @Test
        @DisplayName("실패 - 만료된 토큰은 ExpiredJwtException")
        void expiredToken() {
            // given: 만료 시간을 음수로 줘서 즉시 만료된 토큰 생성
            ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirePeriodMs", -1000L);
            String expired = jwtUtil.generateAccessToken(userDetails("user@example.com"));

            // when & then
            assertThatThrownBy(() -> jwtUtil.validateToken(expired))
                .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("실패 - 다른 secret으로 발급된 토큰은 SignatureException")
        void wrongSignature() {
            // given: 정상 토큰을 만든 뒤 secret을 바꿔치기
            String token = jwtUtil.generateAccessToken(userDetails("user@example.com"));
            ReflectionTestUtils.setField(jwtUtil, "secretKey", "completely-different-secret-key-of-sufficient-length");
            ReflectionTestUtils.setField(jwtUtil, "cachedSecretKey", null); // 캐시 무효화

            // when & then
            assertThatThrownBy(() -> jwtUtil.validateToken(token))
                .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("실패 - 형식이 깨진 토큰은 예외")
        void malformedToken() {
            assertThatThrownBy(() -> jwtUtil.validateToken("not.a.valid.jwt"))
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("getRemainingTime")
    class RemainingTime {

        @Test
        @DisplayName("발급 직후엔 만료 시간 - 1초 보다 큼")
        void positive() {
            String token = jwtUtil.generateAccessToken(userDetails("user@example.com"));

            long remaining = jwtUtil.getRemainingTime(token);

            assertThat(remaining).isPositive().isLessThanOrEqualTo(ACCESS_EXPIRE_MS);
        }
    }

    @Nested
    @DisplayName("getRefreshTokenExpireSeconds")
    class RefreshExpireSeconds {

        @Test
        @DisplayName("ms를 초로 변환해 반환")
        void msToSeconds() {
            long seconds = jwtUtil.getRefreshTokenExpireSeconds();

            assertThat(seconds).isEqualTo(REFRESH_EXPIRE_MS / 1000);
        }
    }
}
