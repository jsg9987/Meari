package com.ssafy.meari.global.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.meari.domain.member.service.MemberService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.auth.request.RefreshTokenRequestDto;
import com.ssafy.meari.global.auth.request.SignupRequestDto;
import com.ssafy.meari.global.auth.response.AccessTokenResponseDto;
import com.ssafy.meari.global.auth.response.EmailCheckResponseDto;
import com.ssafy.meari.global.auth.response.NicknameCheckResponseDto;
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
    private final MemberService memberService;

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

    // 회원가입
    /*@PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequestDto request) {
        memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.successWithoutData());
    }*/

    // 이메일 중복 확인
    @GetMapping("/email/check")
    public ResponseEntity<ApiResponse<EmailCheckResponseDto>> checkEmail(@RequestParam String email) {
        EmailCheckResponseDto response = memberService.checkEmailExists(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 닉네임 중복 확인
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<NicknameCheckResponseDto>> checkNickname(@RequestParam String nickname) {
        NicknameCheckResponseDto response = memberService.checkNicknameExists(nickname);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 회원 탈퇴
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        memberService.deleteMember(userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }
}
