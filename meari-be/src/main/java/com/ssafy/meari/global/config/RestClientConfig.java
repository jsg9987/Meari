package com.ssafy.meari.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${analysis.fastapi.base-url:http://localhost:8000}")
    private String fastApiUrl;

    /**
     * FastAPI 호출용 RestClient
     */
    @Bean
    public RestClient fastApiRestClient() {
        log.info("FastAPI RestClient 생성: baseUrl={}", fastApiUrl);

        return RestClient.builder()
                .baseUrl(fastApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
