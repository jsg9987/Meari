package com.ssafy.meari.global.pipeline.videosaving.anthropic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.global.pipeline.videosaving.anthropic.dto.AnthropicRequestDto;
import com.ssafy.meari.global.pipeline.videosaving.anthropic.dto.AnthropicResponseDto;
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

/**
 * Anthropic Claude API를 사용한 서비스
 * (동음이의어 검증 등에 사용)
 */
@Slf4j
@Service
public class AnthropicService {

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String apiUrl;
	private final String apiKey;
	private final String model;
	private final String apiVersion;

	public AnthropicService(
			@Value("${anthropic.api-url}") String apiUrl,
			@Value("${anthropic.api-key}") String apiKey,
			@Value("${anthropic.model}") String model,
			@Value("${anthropic.api-version:2023-06-01}") String apiVersion,
			ObjectMapper objectMapper
	) {
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();
		this.apiUrl = apiUrl;
		this.apiKey = apiKey;
		this.model = model;
		this.apiVersion = apiVersion;
		this.objectMapper = objectMapper;
	}

	/**
	 * Claude API를 사용한 채팅
	 *
	 * @param systemPrompt 시스템 프롬프트
	 * @param userPrompt 사용자 프롬프트
	 * @return 응답 텍스트
	 */
	public String chat(String systemPrompt, String userPrompt) {
		AnthropicRequestDto requestDto = AnthropicRequestDto.builder()
				.model(model)
				.system(systemPrompt)
				.messages(List.of(
						AnthropicRequestDto.Message.builder()
								.role("user")
								.content(userPrompt)
								.build()
				))
				.max_tokens(4096)
				.build();

		try {
			// 1. DTO를 JSON 문자열로 변환
			String requestBody = objectMapper.writeValueAsString(requestDto);

			// 2. HttpRequest 생성
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(apiUrl))
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header("x-api-key", apiKey)
					.header("anthropic-version", apiVersion)
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.timeout(Duration.ofSeconds(60)) // Claude 응답 대기 시간
					.build();

			// 3. 동기 방식으로 호출
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			// 4. 응답 코드 확인 및 파싱
			if (response.statusCode() == 200) {
				AnthropicResponseDto responseDto = objectMapper.readValue(response.body(), AnthropicResponseDto.class);
				String content = responseDto.getContent();
				return content;
			} else {
				log.error("[Anthropic] API 호출 실패 - 상태 코드: {}, 응답: {}", response.statusCode(), response.body());
				throw new RuntimeException("Claude API 호출 실패: Status " + response.statusCode());
			}

		} catch (Exception e) {
			log.error("[Anthropic] 예외 발생: {}", e.getMessage(), e);
			throw new RuntimeException("Claude API 연동 중 오류 발생", e);
		}
	}

	/**
	 * 기본 시스템 프롬프트와 함께 채팅
	 */
	public String chat(String userPrompt) {
		return chat("You are a helpful assistant.", userPrompt);
	}
}
