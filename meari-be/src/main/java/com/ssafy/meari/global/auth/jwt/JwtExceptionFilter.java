package com.ssafy.meari.global.auth.jwt;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.global.common.ApiResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException e) {
			setErrorResponse(response, ErrorCode.EXPIRED_TOKEN_ERROR);
		} catch (MalformedJwtException e) {
			setErrorResponse(response, ErrorCode.TOKEN_MALFORMED_ERROR);
		} catch (UnsupportedJwtException e) {
			setErrorResponse(response, ErrorCode.TOKEN_UNSUPPORTED_ERROR);
		} catch (IllegalArgumentException e) {
			setErrorResponse(response, ErrorCode.TOKEN_TYPE_ERROR);
		} catch (Exception e) {
			setErrorResponse(response, ErrorCode.TOKEN_UNKNOWN_ERROR);
		}
	}

	private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(errorCode.getHttpStatus().value());

		ApiResponse<Object> failResponse = ApiResponse.fail(new BusinessException(errorCode));
		objectMapper.writeValue(response.getWriter(), failResponse);
	}
}
