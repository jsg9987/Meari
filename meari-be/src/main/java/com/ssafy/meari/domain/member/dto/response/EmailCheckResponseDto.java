package com.ssafy.meari.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmailCheckResponseDto(
	@JsonProperty("has_email")
	boolean hasEmail
) {
}
