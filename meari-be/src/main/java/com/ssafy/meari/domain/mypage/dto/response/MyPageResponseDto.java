package com.ssafy.meari.domain.mypage.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.meari.domain.member.entity.NativeLanguage;

public record MyPageResponseDto(
	String nickname,
	String email,
	@JsonProperty("profile_image_url")
	String profileImageUrl,
	@JsonProperty("native_language")
	NativeLanguage nativeLanguage
) {
}
