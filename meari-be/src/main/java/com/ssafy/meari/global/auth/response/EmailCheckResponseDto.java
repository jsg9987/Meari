package com.ssafy.meari.global.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmailCheckResponseDto(
	@JsonProperty("has_email")
	boolean hasEmail
) {
}
