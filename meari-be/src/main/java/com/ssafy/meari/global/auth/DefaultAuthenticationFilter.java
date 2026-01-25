package com.ssafy.meari.global.auth;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.domain.member.dto.request.LoginRequestDto;
import com.ssafy.meari.global.common.ApiResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import com.ssafy.meari.global.auth.jwt.JwtUtil;
import com.ssafy.meari.global.auth.service.RefreshTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	private static final RequestMatcher DEFAULT_REQUEST_MATCHER =
		PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/members/login");

	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;
	private final RefreshTokenService refreshTokenService;
	private final ObjectMapper objectMapper;

	public DefaultAuthenticationFilter(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
		RefreshTokenService refreshTokenService, ObjectMapper objectMapper) {
		super(DEFAULT_REQUEST_MATCHER);
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
		this.refreshTokenService = refreshTokenService;
		this.objectMapper = objectMapper;
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
		throws AuthenticationException, IOException {

		LoginRequestDto loginDto = objectMapper.readValue(request.getInputStream(), LoginRequestDto.class);
		log.info("로그인 시도 이메일: {}", loginDto.email());

		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
			loginDto.email(),
			loginDto.password()
		);

		return authenticationManager.authenticate(authenticationToken);
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
		Authentication authResult)
		throws IOException, ServletException {

		UserDetailsImpl userDetails = (UserDetailsImpl)authResult.getPrincipal();
		String email = userDetails.getUsername();

		// Access Token, Refresh Token 발급
		String accessToken = jwtUtil.generateAccessToken(userDetails);
		String refreshToken = jwtUtil.generateRefreshToken(userDetails);

		// Refresh Token을 Redis에 저장
		refreshTokenService.saveRefreshToken(email, refreshToken);

		// 응답 헤더에 Refresh Token 추가
		response.setHeader("refresh_token", refreshToken);
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);

		// 응답 본문에 Access Token 추가
		ApiResponse<Map<String, String>> successResponse = ApiResponse.success(Map.of("access_token", accessToken));
		objectMapper.writeValue(response.getWriter(), successResponse);
	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

		ApiResponse<Object> failResponse = ApiResponse.fail(new BusinessException(ErrorCode.FAILURE_LOGIN));
		objectMapper.writeValue(response.getWriter(), failResponse);
	}


}
