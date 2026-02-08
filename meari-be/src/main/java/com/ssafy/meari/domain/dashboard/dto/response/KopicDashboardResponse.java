package com.ssafy.meari.domain.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "코픽 대시보드 응답")
public class KopicDashboardResponse {

    @Schema(description = "최고 점수 정보")
    private BestExamData best;

    @Schema(description = "평균 점수 정보")
    private AverageScoreData average;

    @Getter
    @Builder
    @Schema(description = "최고 점수 시험 데이터")
    public static class BestExamData {

        @JsonProperty("exam_date")
        @Schema(description = "시험 날짜", example = "2026-02-01")
        private LocalDate examDate;

        @JsonProperty("total_avg_score")
        @Schema(description = "총 평균 점수", example = "82")
        private Integer totalAvgScore;

        @Schema(description = "문장별 점수")
        private List<SentenceScore> sentences;
    }

    @Getter
    @Builder
    @Schema(description = "평균 점수 데이터")
    public static class AverageScoreData {

        @JsonProperty("total_avg_score")
        @Schema(description = "총 평균 점수", example = "75")
        private Integer totalAvgScore;

        @Schema(description = "문장별 평균 점수")
        private List<SentenceAvgScore> sentences;
    }

    @Getter
    @Builder
    @Schema(description = "문장별 점수")
    public static class SentenceScore {

        @JsonProperty("kopic_sentence_id")
        @Schema(description = "코픽 문장 ID", example = "1")
        private Long kopicSentenceId;

        @Schema(description = "점수", example = "80")
        private Integer score;
    }

    @Getter
    @Builder
    @Schema(description = "문장별 평균 점수")
    public static class SentenceAvgScore {

        @JsonProperty("kopic_sentence_id")
        @Schema(description = "코픽 문장 ID", example = "1")
        private Long kopicSentenceId;

        @JsonProperty("avg_score")
        @Schema(description = "평균 점수", example = "72")
        private Integer avgScore;
    }
}
