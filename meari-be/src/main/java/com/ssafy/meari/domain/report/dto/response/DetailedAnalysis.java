package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "상세 분석 결과")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DetailedAnalysis {

    @Schema(description = "문장별 분석 결과")
    private List<SentenceAnalysis> sentences;

    @Schema(description = "전체 요약")
    private Summary summary;

    @Schema(description = "문장별 분석")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SentenceAnalysis {

        @Schema(description = "문장 ID", example = "1")
        private Long sentenceId;

        @Schema(description = "기대 텍스트", example = "안녕하세요")
        private String textExpected;

        @Schema(description = "인식된 텍스트", example = "안영하세요")
        private String textRecognized;

        @Schema(description = "정확도", example = "80")
        private Integer accuracy;

        @Schema(description = "평균 신뢰도", example = "0.9739")
        private Double meanConfidence;

        @Schema(description = "음절 목록")
        private List<String> syllables;

        @Schema(description = "음절별 신뢰도")
        private List<Double> syllableConfidences;

        @Schema(description = "오류 목록")
        private List<ErrorDetail> errors;

        @Schema(description = "억양 분석 결과")
        private IntonationAnalysis intonation;
    }

    @Schema(description = "오류 상세")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ErrorDetail {

        @Schema(description = "오류 유형", example = "replace")
        private String type;

        @Schema(description = "위치", example = "1")
        private Integer position;

        @Schema(description = "기대값", example = "녕")
        private String expected;

        @Schema(description = "실제값", example = "영")
        private String actual;

        @Schema(description = "신뢰도", example = "0.8879")
        private Double confidence;

        @Schema(description = "설명", example = "'녕' → '영' 대체")
        private String description;
    }

    @Schema(description = "억양 분석 결과")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class IntonationAnalysis {

        @Schema(description = "억양 점수", example = "85")
        private Integer score;

        @Schema(description = "정답 pitch 배열")
        private List<Double> referencePitch;

        @Schema(description = "사용자 pitch 배열")
        private List<Double> userPitch;

        @Schema(description = "시간 프레임 (초)")
        private List<Double> timeFrames;

        @Schema(description = "DTW 매핑 경로")
        private List<List<Integer>> dtwPath;

        @Schema(description = "피드백 메시지", example = "억양이 매우 자연스럽습니다!")
        private String feedback;
    }

    @Schema(description = "전체 요약")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Summary {

        @Schema(description = "전체 문장 수", example = "10")
        private Integer totalSentences;

        @Schema(description = "분석 완료된 문장 수", example = "10")
        private Integer analyzedSentences;

        @Schema(description = "평균 정확도", example = "85")
        private Integer averageAccuracy;

        @Schema(description = "평균 신뢰도", example = "0.9234")
        private Double averageConfidence;

        @Schema(description = "평균 억양 점수", example = "85")
        private Integer averageIntonation;
    }
}
