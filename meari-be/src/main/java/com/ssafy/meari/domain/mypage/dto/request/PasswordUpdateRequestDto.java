package com.ssafy.meari.domain.mypage.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

//TODO 비밀번호 패턴 추가
public record PasswordUpdateRequestDto(
	@JsonProperty("new_password")
	String newPassword
) {
}
