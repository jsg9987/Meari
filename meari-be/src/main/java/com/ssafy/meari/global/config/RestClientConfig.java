package com.ssafy.meari.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * RestClient 설정
 * - FastAPI 직접 호출을 위한 RestClient 빈 생성
 * - Spring Boot 3.2+ RestClient 사용 (WebFlux 의존성 불필요)
 */
@Slf4j
@Configuration
public class RestClientConfig {

    private static final String FAST_API_URL = "http://localhost:8000";

    /**
     * FastAPI 호출용 RestClient
     */
    @Bean
    public RestClient fastApiRestClient() {
        log.info("FastAPI RestClient 생성: baseUrl={}", FAST_API_URL);

        return RestClient.builder()
                .baseUrl(FAST_API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
