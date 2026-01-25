package com.ssafy.meari.global.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
	// TODO redis로 만료된 토큰 관리하는 기능 구현 예정

	public boolean isBlacklisted(String accessToken) {
		return false; // TODO 구현 예정
	}

}
