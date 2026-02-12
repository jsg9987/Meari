package com.ssafy.meari.domain.analysis.service;

import com.ssafy.meari.domain.analysis.dto.AnalysisRequestMessage;
import com.ssafy.meari.global.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 방식 발음 분석 서비스
 * - 기존 방식 유지 (Spring Boot → RabbitMQ → FastAPI)
 */
@Slf4j
@Component("rabbitMQAnalysisService")
@RequiredArgsConstructor
public class RabbitMQAnalysisService implements AnalysisService {

    private final RabbitTemplate rabbitTemplate;
    private final AnalysisRequestBuilder requestBuilder;

    @Override
    public void requestMemberAnalysis(Long roomId, Integer round, Long memberId) {
        log.info("[RabbitMQ] 발음 분석 요청: roomId={}, round={}, memberId={}", roomId, round, memberId);

        try {
            // 1. 분석 요청 데이터 생성 (공통 로직)
            AnalysisRequestMessage message = requestBuilder.buildRequest(roomId, round, memberId);

            // 2. RabbitMQ로 메시지 발행
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ANALYSIS_EXCHANGE,
                    RabbitMQConfig.REQUEST_ROUTING_KEY,
                    message
            );

            log.info("[RabbitMQ] 발음 분석 요청 발행 완료: roomId={}, round={}, memberId={}, sentences={}",
                    roomId, round, memberId, message.getSentences().size());

        } catch (Exception e) {
            log.error("[RabbitMQ] 발음 분석 요청 실패: roomId={}, round={}, memberId={}",
                    roomId, round, memberId, e);
            throw e;
        }
    }
}
