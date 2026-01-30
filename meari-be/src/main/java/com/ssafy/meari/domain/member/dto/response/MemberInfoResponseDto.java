package com.ssafy.meari.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MemberInfoResponseDto(
	Long memberId,
	String email,
	@JsonProperty("profile_url")
	String profileUrl,
	String nickname
) {
}
