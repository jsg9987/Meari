package com.ssafy.meari.global.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ssafy.meari.global.auth.UserDetailsImpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

@Component
public class JwtUtil {
    // 추후 JwtProperties를 이용한 자바 객체 기반 설정으로 리팩토링 가능 (@Value를 통한 바인딩 x)

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.access-token-expire-period}")
    private long accessTokenExpirePeriod;

    @Value("${jwt.refresh-token-expire-period}")
    private long refreshTokenExpirePeriod;

    private SecretKey cachedSecretKey;

    // 항상 같은 secretKey를 사용하기에 서명 결과도 동일
    private SecretKey getSecretKey() {
        if (cachedSecretKey == null) {
            cachedSecretKey
                = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key()
                    .build()
                    .getAlgorithm());
        }
        return cachedSecretKey;
    }

    // TODO 현재의 방식은 지연생성방식으로, 멀티스레드 환경에서
    //  중복 생성을 막으려면 synchronized 처리를 고려해봐야 함
    // Access Token 생성
    public String generateAccessToken(UserDetailsImpl userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirePeriod);

        return Jwts.builder()
                .subject(userDetails.getUsername()) // email
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSecretKey())
                .compact();
    }

    // Refresh Token 생성
    public String generateRefreshToken(UserDetailsImpl userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpirePeriod);

        return Jwts.builder()
                .subject(userDetails.getUsername()) // email
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSecretKey())
                .compact();
    }

    // 토큰 유효성 검증
    public Claims validateToken(String token) throws ExpiredJwtException {
        return Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token) // 토큰 검증 실패 시 예외 던짐
            .getPayload();
    }

    // 토큰의 남은 유효 시간 계산 (밀리초)
    public long getRemainingTime(String token) {
        Claims claims = validateToken(token);
        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        return expiration.getTime() - now;
    }

}
