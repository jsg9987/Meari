package com.ssafy.meari.global.auth.service;

import org.springframework.stereotype.Service;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.mapper.MemberMapper;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.auth.jwt.JwtUtil;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
	private final JwtUtil jwtUtil;
	private final RefreshTokenService refreshTokenService;
	private final TokenBlacklistService tokenBlacklistService;
	private final MemberMapper memberMapper;
	private final MemberRepository memberRepository;

	// Refresh Token으로 Access Token 재발급
	public String refreshAccessToken(String refreshToken) {
		try {
			// Refresh Token 검증
			Claims claims = jwtUtil.validateToken(refreshToken);
			String email = claims.getSubject();

			// Redis에 저장된 Refresh Token과 비교
			String storedToken = refreshTokenService.getRefreshToken(email);
			if (storedToken == null) {
				throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
			}

			if (!storedToken.equals(refreshToken)) {
				throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
			}

			// DB에서 사용자 정보 조회
			Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

			// UserDetailsImpl 생성 및 Access Token 발급
			UserDetailsImpl userDetails = new UserDetailsImpl(member);
			String accessToken = jwtUtil.generateAccessToken(userDetails);

			log.info("Access token 재발급 완료: {}", email);

			return accessToken;

		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("Refresh token 검증 실패", e);
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
	}

	// 로그아웃
	public void logout(String email, String accessToken) {
		// Access Token을 블랙리스트에 추가
		tokenBlacklistService.addToBlacklist(accessToken);

		// Refresh Token을 Redis에서 삭제
		refreshTokenService.deleteRefreshToken(email);

		log.info("로그아웃: {}", email);
	}
}
