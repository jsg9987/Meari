package com.ssafy.meari.global.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.auth.request.RefreshTokenRequestDto;
import com.ssafy.meari.global.auth.response.AccessTokenResponseDto;
import com.ssafy.meari.global.auth.service.AuthService;
import com.ssafy.meari.global.common.ApiResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Refresh Token으로 Access Token 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponseDto>> refresh(@Valid @RequestBody RefreshTokenRequestDto requestDto) {
        String accessToken = authService.refreshAccessToken(requestDto.getRefreshToken());

        return ResponseEntity
            .ok(ApiResponse.success(new AccessTokenResponseDto(accessToken)));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetailsImpl userDetails, HttpServletRequest request) {
        String email = userDetails.getUsername();

        // Authorization 헤더에서 Access Token 추출
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NOT_FOUND_AUTHORIZATION_HEADER);
        }

        String accessToken = authorizationHeader.substring(7);

        // 로그아웃 처리
        authService.logout(email, accessToken);

        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }


}
