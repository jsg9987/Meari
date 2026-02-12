package com.ssafy.meari.domain.kopic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.meari.domain.report.entity.KopicReport;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.repository.KopicReportRepository;
import com.ssafy.meari.domain.report.repository.KopicTotalReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KopicAggregationService {

    private final KopicReportRepository kopicReportRepository;
    private final KopicTotalReportRepository kopicTotalReportRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void tryAggregate(Long kopicTotalReportId) {
        KopicTotalReport totalReport = kopicTotalReportRepository.findById(kopicTotalReportId)
                .orElse(null);

        if (totalReport == null) {
            log.error("통합 리포트를 찾을 수 없습니다: totalReportId={}", kopicTotalReportId);
            return;
        }

        if (totalReport.getStatus() == ReportStatus.COMPLETED) {
            log.debug("이미 집계 완료된 리포트: totalReportId={}", kopicTotalReportId);
            return;
        }

        long totalCount = kopicReportRepository.countByKopicTotalReport(totalReport);
        long completedCount = kopicReportRepository.countByKopicTotalReportAndStatus(totalReport, ReportStatus.COMPLETED);

        if (totalCount == 0 || totalCount != completedCount) {
            return;
        }

        List<KopicReport> reports = kopicReportRepository
                .findByKopicTotalReportOrderByKopicSentence_KopicSentenceIdAsc(totalReport);

        int avgAccuracy = (int) reports.stream().mapToInt(KopicReport::getAccuracy).average().orElse(0);
        int totalScore = avgAccuracy;

        String reportData = buildReportData(reports);

        totalReport.updateAggregation(avgAccuracy, totalScore, reports.size(), reportData);

        log.debug("코픽 세션 집계 완료: totalReportId={}, sentenceCount={}, totalScore={}",
                kopicTotalReportId, reports.size(), totalScore);
    }

    private String buildReportData(List<KopicReport> reports) {
        try {
            ArrayNode arrayNode = objectMapper.createArrayNode();

            for (KopicReport report : reports) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("kopic_report_id", report.getKopicReportId());
                node.put("kopic_sentence_id", report.getKopicSentence().getKopicSentenceId());
                node.put("text_ko", report.getKopicSentence().getTextKo());
                node.put("accuracy", report.getAccuracy());
                node.put("total_score", report.getTotalScore());

                if (report.getDetailedAnalysis() != null) {
                    node.set("detailed_analysis", objectMapper.readTree(report.getDetailedAnalysis()));
                }

                arrayNode.add(node);
            }

            return objectMapper.writeValueAsString(arrayNode);
        } catch (Exception e) {
            throw new RuntimeException("리포트 데이터 직렬화 실패", e);
        }
    }
}
