package com.ssafy.meari.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NicknameCheckResponseDto(
	@JsonProperty("has_nickname")
	boolean hasNickname
) {
}
