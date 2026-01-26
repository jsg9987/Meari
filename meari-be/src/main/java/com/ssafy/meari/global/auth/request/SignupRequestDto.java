package com.ssafy.meari.global.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.meari.domain.member.entity.NativeLanguage;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//TODO 비밀번호/닉네임 패턴 추가
public record SignupRequestDto(
	@Email
	@NotBlank
	String email,

	@NotBlank
	String password,

	@NotBlank
	String nickname,

	@JsonProperty("native_language") // (역)직렬화 필드명 매핑
	@NotNull
	NativeLanguage nativeLanguage
) {}
