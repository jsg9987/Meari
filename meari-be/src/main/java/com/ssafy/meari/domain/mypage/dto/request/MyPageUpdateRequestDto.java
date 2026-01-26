package com.ssafy.meari.domain.mypage.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.meari.domain.member.entity.NativeLanguage;

public record MyPageUpdateRequestDto(
	String nickname,
	@JsonProperty("native_language")
	NativeLanguage nativeLanguage
) {
}
