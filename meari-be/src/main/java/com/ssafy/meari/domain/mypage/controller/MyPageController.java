package com.ssafy.meari.domain.mypage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
