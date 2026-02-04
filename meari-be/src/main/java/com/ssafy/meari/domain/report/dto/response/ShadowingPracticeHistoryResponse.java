package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Schema(description = "쉐도잉 연습 이력 응답")
@Getter
@Builder
@Slf4j
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ShadowingPracticeHistoryResponse {

    @Schema(description = "리스트 인덱스", example = "1")
    private Integer idx;

    @Schema(description = "연습 날짜", example = "2026-02-02")
    private String date;

    @Schema(description = "정확도 점수", example = "85")
    private Integer accuracy;

    @Schema(description = "억양 점수", example = "90")
    private Integer intonation;

    @Schema(description = "오류 개수", example = "1")
    private Integer errorsCount;

    /**
     * ShadowingReport를 ShadowingPracticeHistoryResponse로 변환
     *
     * @param report ShadowingReport 엔티티
     * @param idx 리스트 인덱스 (1부터 시작)
     * @return 변환된 응답 DTO
     */
    public static ShadowingPracticeHistoryResponse from(ShadowingReport report, Integer idx) {
        // date 변환: LocalDateTime → String (YYYY-MM-DD)
        LocalDate practiceDate = report.getCreatedAt().toLocalDate();
        String dateString = practiceDate.toString();

        // errorsCount 계산: detailedAnalysis JSON 파싱
        Integer errorsCount = calculateErrorsCount(report.getDetailedAnalysis());

        return ShadowingPracticeHistoryResponse.builder()
                .idx(idx)
                .date(dateString)
                .accuracy(report.getAccuracy())
                .intonation(report.getIntonation())
                .errorsCount(errorsCount)
                .build();
    }

    /**
     * detailedAnalysis JSON에서 총 오류 개수 계산
     *
     * @param detailedAnalysisJson JSONB 문자열
     * @return 총 오류 개수 (파싱 실패 시 0)
     */
    private static Integer calculateErrorsCount(String detailedAnalysisJson) {
        if (detailedAnalysisJson == null || detailedAnalysisJson.isBlank()) {
            return 0;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            DetailedAnalysis detailedAnalysis = objectMapper.readValue(
                    detailedAnalysisJson,
                    DetailedAnalysis.class
            );

            if (detailedAnalysis.getSentences() == null || detailedAnalysis.getSentences().isEmpty()) {
                return 0;
            }

            // 모든 문장의 errors 개수 합산
            int totalErrors = 0;
            for (DetailedAnalysis.SentenceAnalysis sentence : detailedAnalysis.getSentences()) {
                if (sentence.getErrors() != null) {
                    totalErrors += sentence.getErrors().size();
                }
            }

            return totalErrors;
        } catch (Exception e) {
            log.debug("DetailedAnalysis JSON 파싱 실패: {}", e.getMessage());
            return 0;
        }
    }
}
