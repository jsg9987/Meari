package com.ssafy.meari.global.pipeline.videosaving.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Anthropic Claude API 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnthropicResponseDto {

	private String id;
	private String type;
	private String model;
	@JsonProperty("stop_reason")
	private String stopReason;
	@JsonProperty("stop_sequence")
	private String stopSequence;
	private List<ContentBlock> content;
	private Usage usage;

	/**
	 * 응답 내용 추출
	 */
	public String getContent() {
		if (content == null || content.isEmpty()) {
			return "";
		}
		// 첫 번째 content 블록의 text 추출
		return content.get(0).getText();
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ContentBlock {
		private String type;
		private String text;

		public String getText() {
			return text != null ? text : "";
		}
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Usage {
		@JsonProperty("input_tokens")
		private Integer inputTokens;
		@JsonProperty("output_tokens")
		private Integer outputTokens;
	}
}
