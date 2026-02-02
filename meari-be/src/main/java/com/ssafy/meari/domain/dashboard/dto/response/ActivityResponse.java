package com.ssafy.meari.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "일일 학습 기록 응답")
public class ActivityResponse {

    @Schema(description = "기록 날짜", example = "2025-02-02")
    private String date;

    @Schema(description = "학습 레벨 (0~2)", example = "2")
    private Integer level;

    @Schema(description = "단어 학습 완료 여부", example = "true")
    private Boolean wordStudy;

    @Schema(description = "문장 퀴즈 완료 여부", example = "true")
    private Boolean sentenceQuiz;
}
