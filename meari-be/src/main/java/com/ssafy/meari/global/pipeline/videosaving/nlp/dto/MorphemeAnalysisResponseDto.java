package com.ssafy.meari.global.pipeline.videosaving.nlp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "형태소 분석 결과")
public class MorphemeAnalysisResponseDto {

    @Schema(description = "원본 문장", example = "안녕하세요")
    private String originalText;

    @Schema(description = "형태소 분석 결과 목록")
    private List<Morpheme> morphemes;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "형태소 정보")
    public static class Morpheme {

        @Schema(description = "형태소", example = "안녕")
        private String text;

        @Schema(description = "품사", example = "Noun")
        private String pos;

        @Schema(description = "확률", example = "1.0")
        private Double confidence;
    }
}
