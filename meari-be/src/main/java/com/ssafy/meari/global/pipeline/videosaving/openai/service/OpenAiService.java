package com.ssafy.meari.global.pipeline.videosaving.openai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.global.pipeline.videosaving.openai.dto.OpenAiRequestDto;
import com.ssafy.meari.global.pipeline.videosaving.openai.dto.OpenAiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class OpenAiService {

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String apiUrl;
	private final String apiKey;
	private final String model;

	public OpenAiService(
			@Value("${openai.api-url}") String apiUrl,
			@Value("${openai.api-key}") String apiKey,
			@Value("${openai.model}") String model,
			ObjectMapper objectMapper
	) {
		// HttpClient 설정 (타임아웃 등 설정 가능)
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();
		this.apiUrl = apiUrl;
		this.apiKey = apiKey;
		this.model = model;
		this.objectMapper = objectMapper;
	}

	public String chat(String systemPrompt, String userPrompt) {
		log.debug("[OpenAI] API 호출 시작 (HttpClient)");

		OpenAiRequestDto requestDto = OpenAiRequestDto.builder()
				.model(model)
				.messages(List.of(
						OpenAiRequestDto.Message.builder()
								.role("developer")
								.content(systemPrompt)
								.build(),
						OpenAiRequestDto.Message.builder()
								.role("user")
								.content(userPrompt)
								.build()
				))
				.build();

		try {
			// 1. DTO를 JSON 문자열로 변환
			String requestBody = objectMapper.writeValueAsString(requestDto);

			// 2. HttpRequest 생성
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(apiUrl))
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.timeout(Duration.ofSeconds(30)) // AI 응답 대기 시간
					.build();

			// 3. 동기 방식으로 호출
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			// 4. 응답 코드 확인 및 파싱
			if (response.statusCode() == 200) {
				OpenAiResponseDto responseDto = objectMapper.readValue(response.body(), OpenAiResponseDto.class);
				String content = responseDto.getContent();
				log.debug("[OpenAI] API 호출 성공 - 응답 길이: {}", content != null ? content.length() : 0);
				return content;
			} else {
				log.error("[OpenAI] API 호출 실패 - 상태 코드: {}, 응답: {}", response.statusCode(), response.body());
				throw new RuntimeException("OpenAI API 호출 실패: Status " + response.statusCode());
			}

		} catch (Exception e) {
			log.error("[OpenAI] 예외 발생: {}", e.getMessage(), e);
			throw new RuntimeException("OpenAI 연동 중 오류 발생", e);
		}
	}

	public String chat(String userPrompt) {
		return chat("You are a helpful assistant.", userPrompt);
	}
}
