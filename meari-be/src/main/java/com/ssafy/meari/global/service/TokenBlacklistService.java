package com.ssafy.meari.global.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.ssafy.meari.global.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
	private final RedisTemplate<String, String> redisTemplate;
	private final JwtUtil jwtUtil;

	// Access Token을 블랙리스트에 추가
	public void addToBlacklist(String token) {
		// 토큰의 남은 유효 시간 계산
		long remainingTime = jwtUtil.getRemainingTime(token);

		// 남은 시간이 0보다 클 때만 블랙리스트에 추가
		if (remainingTime > 0) {
			redisTemplate.opsForValue().set(
				"blacklist:" + token,
				"logout",
				remainingTime,
				TimeUnit.MILLISECONDS
			);
		}
	}

	// 블랙리스트 확인
	public boolean isBlacklisted(String token) {
		return redisTemplate.hasKey("blacklist:" + token);
	}

}
