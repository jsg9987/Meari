package com.ssafy.meari.domain.member.controller;

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

import com.ssafy.meari.domain.member.dto.request.SignupRequestDto;
import com.ssafy.meari.domain.member.dto.response.EmailCheckResponseDto;
import com.ssafy.meari.domain.member.dto.response.MemberInfoResponseDto;
import com.ssafy.meari.domain.member.dto.response.NicknameCheckResponseDto;
import com.ssafy.meari.domain.member.service.MemberService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequestDto request) {
		memberService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.successWithoutData());
	}

	@GetMapping("/email/check")
	public ResponseEntity<ApiResponse<EmailCheckResponseDto>> checkEmail(@RequestParam String email) {
		EmailCheckResponseDto response = memberService.checkEmailExists(email);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/nickname/check")
	public ResponseEntity<ApiResponse<NicknameCheckResponseDto>> checkNickname(@RequestParam String nickname) {
		NicknameCheckResponseDto response = memberService.checkNicknameExists(nickname);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@DeleteMapping("/delete")
	public ResponseEntity<ApiResponse<Void>> deleteMember(@AuthenticationPrincipal UserDetailsImpl userDetails) {
		memberService.deleteMember(userDetails.getMember().getMemberId());
		return ResponseEntity.ok(ApiResponse.successWithoutData());
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<MemberInfoResponseDto>> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
		MemberInfoResponseDto response = memberService.getMyInfo(userDetails.getMember().getMemberId());
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
