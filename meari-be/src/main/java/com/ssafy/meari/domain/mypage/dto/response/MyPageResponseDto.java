package com.ssafy.meari.domain.mypage.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MyPageResponseDto(
	String nickname,
	String email,
	@JsonProperty("profile_image_url")
	String profileImageUrl,
	@JsonProperty("native_language")
	String nativeLanguage
) {
}
