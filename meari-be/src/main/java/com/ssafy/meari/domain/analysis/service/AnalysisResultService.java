package com.ssafy.meari.domain.analysis.service;

import com.ssafy.meari.domain.analysis.dto.AnalysisResultMessage;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분석 결과 업데이트 서비스
 * - ShadowingReport DB 업데이트 전용
 * - @Transactional self-invocation 문제 해결을 위해 별도 서비스로 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisResultService {

    private final ShadowingReportRepository shadowingReportRepository;

    /**
     * ShadowingReport 업데이트 (분석 결과 반영)
     *
     * @param result FastAPI로부터 받은 분석 결과
     */
    @Transactional
    public void updateShadowingReport(AnalysisResultMessage result) {
        log.debug("ShadowingReport 업데이트 시작: roomId={}, round={}, memberId={}",
                result.getRoomId(), result.getRound(), result.getMemberId());

        ShadowingReport report = shadowingReportRepository
                .findByRoom_RoomIdAndRoundAndMember_MemberId(
                        result.getRoomId(),
                        result.getRound(),
                        result.getMemberId()
                )
                .orElseThrow(() -> {
                    log.error("ShadowingReport 없음: roomId={}, round={}, memberId={}",
                            result.getRoomId(), result.getRound(), result.getMemberId());
                    return new BusinessException(ErrorCode.NOT_FOUND_REPORT);
                });

        // Dirty Checking으로 자동 업데이트
        report.updateAnalysisResult(
                result.getAccuracy(),
                result.getIntonation(),
                result.getDetailedAnalysis()
        );

        log.info("ShadowingReport 업데이트 완료: reportId={}, accuracy={}, intonation={}",
                report.getShadowingReportId(), result.getAccuracy(), result.getIntonation());
    }
}
