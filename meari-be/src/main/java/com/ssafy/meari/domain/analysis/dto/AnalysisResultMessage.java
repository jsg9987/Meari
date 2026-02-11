package com.ssafy.meari.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "발음 분석 결과 메시지 (RabbitMQ)")
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AnalysisResultMessage {

    @Schema(description = "방 ID", example = "123")
    private Long roomId;

    @Schema(description = "라운드 번호", example = "1")
    private Integer round;

    @Schema(description = "멤버 ID", example = "1")
    private Long memberId;

    @Schema(description = "정확도 점수 (0-100)", example = "85")
    private Integer accuracy;

    @Schema(description = "억양 점수 (0-100)", example = "90")
    private Integer intonation;

    @Schema(description = "상세 분석 결과 (JSON 문자열)", example = "{\"errors\": []}")
    private String detailedAnalysis;

    @Builder
    public AnalysisResultMessage(Long roomId, Integer round, Long memberId,
                                 Integer accuracy, Integer intonation,
                                 String detailedAnalysis) {
        this.roomId = roomId;
        this.round = round;
        this.memberId = memberId;
        this.accuracy = accuracy;
        this.intonation = intonation;
        this.detailedAnalysis = detailedAnalysis;
    }
}
