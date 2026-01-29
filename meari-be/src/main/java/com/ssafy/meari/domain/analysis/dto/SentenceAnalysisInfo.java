package com.ssafy.meari.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "문장별 분석 정보")
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SentenceAnalysisInfo {

    @Schema(description = "문장 ID", example = "1")
    private Long sentenceId;

    @Schema(description = "오디오 파일 URL", example = "s3://meari-bucket/recordings/room123/round1/member1/sentence1.wav")
    private String audioUrl;

    @Schema(description = "한국어 텍스트", example = "안녕하세요")
    private String textKo;

    @Schema(description = "시작 시간 (초)", example = "0.5")
    private Double startTime;

    @Schema(description = "종료 시간 (초)", example = "2.3")
    private Double endTime;

    @Builder
    public SentenceAnalysisInfo(Long sentenceId, String audioUrl, String textKo,
                                Double startTime, Double endTime) {
        this.sentenceId = sentenceId;
        this.audioUrl = audioUrl;
        this.textKo = textKo;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
