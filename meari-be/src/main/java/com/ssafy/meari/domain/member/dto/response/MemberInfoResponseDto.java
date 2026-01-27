package com.ssafy.meari.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MemberInfoResponseDto(
	String email,
	@JsonProperty("profile_url")
	String profileUrl,
	String nickname
) {
}
