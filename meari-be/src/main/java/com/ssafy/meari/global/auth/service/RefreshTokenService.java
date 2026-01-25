package com.ssafy.meari.global.auth.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RedisTemplate<String, String> redisTemplate;

	@Value("${jwt.refresh-token-expire-period}")
	private Long refreshTokenExpirePeriod;

	// Refresh Token 저장
	public void saveRefreshToken(String email, String refreshToken) {
		redisTemplate.opsForValue().set(
			"refresh:" + email,
			refreshToken,
			refreshTokenExpirePeriod,
			TimeUnit.MILLISECONDS
		);
	}

	// Refresh Token 조회
	public String getRefreshToken(String email) {
		return redisTemplate.opsForValue().get("refresh:" + email);
	}

	// Refresh Token 삭제 (로그아웃)
	public void deleteRefreshToken(String email) {
		redisTemplate.delete("refresh:" + email);
	}

	// Refresh Token 검증
	public boolean validateRefreshToken(String email, String refreshToken) {
		String storedToken = getRefreshToken(email);
		return refreshToken.equals(storedToken);
	}
}
