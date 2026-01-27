package com.ssafy.meari.domain.mypage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.meari.domain.mypage.dto.request.MyPageUpdateRequestDto;
import com.ssafy.meari.domain.mypage.dto.request.PasswordCheckRequestDto;
import com.ssafy.meari.domain.mypage.dto.request.PasswordUpdateRequestDto;
import com.ssafy.meari.domain.mypage.dto.response.MyPageResponseDto;
import com.ssafy.meari.domain.mypage.service.MyPageService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MyPageController {

	private final MyPageService myPageService;

	@GetMapping
	public ResponseEntity<ApiResponse<MyPageResponseDto>> getMyPage(@AuthenticationPrincipal UserDetailsImpl userDetails) {
		MyPageResponseDto response = myPageService.getMyPage(userDetails.getMember().getMemberId());
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@PatchMapping("/change")
	public ResponseEntity<ApiResponse<Void>> updateMyPage(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@RequestBody MyPageUpdateRequestDto request) {
		myPageService.updateMyPage(userDetails.getMember().getMemberId(), request);
		return ResponseEntity.ok(ApiResponse.successWithoutData());
	}

	@PatchMapping("/change/pw")
	public ResponseEntity<ApiResponse<Void>> updatePassword(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@RequestBody PasswordUpdateRequestDto request) {
		myPageService.updatePassword(userDetails.getMember().getMemberId(), request);
		return ResponseEntity.ok(ApiResponse.successWithoutData());
	}

	@GetMapping("/check-password")
	public ResponseEntity<ApiResponse<Void>> checkPassword(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@RequestBody PasswordCheckRequestDto request) {
		myPageService.checkPassword(userDetails.getMember().getMemberId(), request);
		return ResponseEntity.ok(ApiResponse.successWithoutData());
	}
}
