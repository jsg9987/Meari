package com.ssafy.meari.global.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NicknameCheckResponseDto(
	@JsonProperty("has_nickname")
	boolean hasNickname
) {
}
