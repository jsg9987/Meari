package com.ssafy.meari.global.pipeline.videosaving.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiRequestDto {

	private String model;
	private List<Message> messages;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Message {
		private String role;
		private String content;
	}
}
