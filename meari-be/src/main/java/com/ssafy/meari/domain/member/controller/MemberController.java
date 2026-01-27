package com.ssafy.meari.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.meari.domain.member.dto.response.MemberInfoResponseDto;
import com.ssafy.meari.domain.member.service.MemberService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<MemberInfoResponseDto>> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
		MemberInfoResponseDto response = memberService.getMyInfo(userDetails.getMember().getMemberId());
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
