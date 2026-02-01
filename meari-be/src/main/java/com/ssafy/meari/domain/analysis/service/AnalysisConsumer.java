package com.ssafy.meari.domain.analysis.service;

import com.ssafy.meari.domain.analysis.dto.AnalysisResultMessage;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.global.config.RabbitMQConfig;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisConsumer {

    private final ShadowingReportRepository shadowingReportRepository;

    /**
     * FastAPI로부터 발음 분석 결과 수신
     * ShadowingReport 업데이트 및 상태 변경 (PROCESSING → COMPLETED)
     */
    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    @Transactional
    public void handleAnalysisResult(AnalysisResultMessage message) {
        log.info("발음 분석 결과 수신: roomId={}, round={}, memberId={}, accuracy={}, intonation={}",
                message.getRoomId(), message.getRound(), message.getMemberId(),
                message.getAccuracy(), message.getIntonation());

        try {
            // 1. ShadowingReport 조회
            ShadowingReport report = shadowingReportRepository
                    .findByRoom_RoomIdAndRoundAndMember_MemberId(
                            message.getRoomId(),
                            message.getRound(),
                            message.getMemberId()
                    )
                    .orElseThrow(() -> {
                        log.error("ShadowingReport 없음: roomId={}, round={}, memberId={}",
                                message.getRoomId(), message.getRound(), message.getMemberId());
                        return new BusinessException(ErrorCode.NOT_FOUND_REPORT);
                    });

            // 2. 분석 결과 업데이트 (Dirty Checking 활용)
            report.updateAnalysisResult(
                    message.getAccuracy(),
                    message.getIntonation(),
                    message.getDetailedAnalysis()
            );

            log.info("ShadowingReport 업데이트 완료: reportId={}, status=COMPLETED",
                    report.getShadowingReportId());

        } catch (Exception e) {
            log.error("발음 분석 결과 처리 실패: {}", e.getMessage(), e);

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
                log.error("실패 상태 업데이트도 실패: {}", innerEx.getMessage());
            }
        }
    }
}
