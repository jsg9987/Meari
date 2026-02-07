package com.ssafy.meari.domain.analysis.service;

import com.ssafy.meari.domain.analysis.dto.AnalysisResultMessage;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.global.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ 분석 결과 Consumer
 * - FastAPI로부터 분석 결과를 수신하여 DB 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisConsumer {

    private final AnalysisResultService analysisResultService;
    private final ShadowingReportRepository shadowingReportRepository;

    /**
     * FastAPI로부터 발음 분석 결과 수신
     * ShadowingReport 업데이트 및 상태 변경 (PROCESSING → COMPLETED)
     */
    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    public void handleAnalysisResult(AnalysisResultMessage message) {
        log.info("[RabbitMQ] 발음 분석 결과 수신: roomId={}, round={}, memberId={}, accuracy={}, intonation={}",
                message.getRoomId(), message.getRound(), message.getMemberId(),
                message.getAccuracy(), message.getIntonation());

        try {
            analysisResultService.updateShadowingReport(message);

            log.info("[RabbitMQ] ShadowingReport 업데이트 완료");

        } catch (Exception e) {
            log.error("[RabbitMQ] 발음 분석 결과 처리 실패: {}", e.getMessage(), e);

            // 실패 시 상태 업데이트 시도
            try {
                shadowingReportRepository
                        .findByRoom_RoomIdAndRoundAndMember_MemberId(
                                message.getRoomId(),
                                message.getRound(),
                                message.getMemberId()
                        )
                        .ifPresent(ShadowingReport::markAsFailed);
            } catch (Exception innerEx) {
                log.error("[RabbitMQ] 실패 상태 업데이트도 실패: {}", innerEx.getMessage());
            }
        }
    }
}
