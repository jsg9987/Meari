package com.ssafy.meari.global.pipeline.videosaving.anthropic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Anthropic Claude API 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnthropicRequestDto {

	private String model;
	private String system;
	private List<Message> messages;
	private Integer max_tokens;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Message {
		private String role;
		private String content;
	}
}
