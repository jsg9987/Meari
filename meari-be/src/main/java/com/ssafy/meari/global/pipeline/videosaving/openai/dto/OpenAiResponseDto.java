package com.ssafy.meari.global.pipeline.videosaving.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiResponseDto {

	private String id;
	private String object;
	private Long created;
	private String model;
	private List<Choice> choices;
	private Usage usage;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Choice {
		private Integer index;
		private Message message;
		@JsonProperty("finish_reason")
		private String finishReason;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Message {
		private String role;
		private String content;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Usage {
		@JsonProperty("prompt_tokens")
		private Integer promptTokens;
		@JsonProperty("completion_tokens")
		private Integer completionTokens;
		@JsonProperty("total_tokens")
		private Integer totalTokens;
	}

	/**
	 * 첫 번째 응답 메시지의 content 반환
	 */
	public String getContent() {
		if (choices != null && !choices.isEmpty()) {
			return choices.get(0).getMessage().getContent();
		}
		return null;
	}
}
