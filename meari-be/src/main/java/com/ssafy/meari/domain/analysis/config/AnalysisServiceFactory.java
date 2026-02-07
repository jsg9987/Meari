package com.ssafy.meari.domain.analysis.config;

import com.ssafy.meari.domain.analysis.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 분석 서비스 팩토리
 * - application.yml 설정에 따라 HTTP 또는 RabbitMQ 방식 선택
 */
@Slf4j
@Configuration
public class AnalysisServiceFactory {

    @Value("${analysis.mode:http}")
    private String analysisMode;

    /**
     * 분석 서비스 빈 생성
     * - analysis.mode=http → HttpAnalysisService
     * - analysis.mode=rabbitmq → RabbitMQAnalysisService
     */
    @Bean
    public AnalysisService analysisService(
            @Qualifier("httpAnalysisService") AnalysisService httpService,
            @Qualifier("rabbitMQAnalysisService") AnalysisService rabbitMQService) {

        if ("http".equalsIgnoreCase(analysisMode)) {
            log.info("=== HTTP 분석 모드 활성화 ===");
            log.info("Spring Boot → FastAPI 직접 호출 방식 사용");
            return httpService;
        } else if ("rabbitmq".equalsIgnoreCase(analysisMode)) {
            log.info("=== RabbitMQ 분석 모드 활성화 ===");
            log.info("Spring Boot → RabbitMQ → FastAPI 방식 사용");
            return rabbitMQService;
        } else {
            log.warn("알 수 없는 분석 모드: {}. HTTP 모드로 기본 설정됨", analysisMode);
            return httpService;
        }
    }
}
