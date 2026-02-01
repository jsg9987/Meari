package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.KopicReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "코픽 리포트 상세 항목 (report_data 배열의 각 항목)")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KopicReportDetailItemResponse {

    @Schema(description = "코픽 리포트 ID", example = "1001")
    private Long kopicReportId;

    @Schema(description = "코픽 문장 ID", example = "10")
    private Long kopicSentenceId;

    @Schema(description = "문장 텍스트 (한국어)", example = "카페 주문 상황 - 안녕하세요. 아이스 아메리카노 한 잔이랑 치즈케이크 하나 주세요.")
    private String textKo;

    @Schema(description = "응답 내용의 적절성(질문의 의도에 알맞은 정보를 제공했는지)", example = "85")
    private Integer accuracy;

    @Schema(description = "총 점수", example = "82") // TODO 코픽의 경우 평가항목이 하나이므로, 필드 없애도 될듯.
    private Integer totalScore;

    @Schema(description = "상세 분석 결과 (JSONB) - missed_point, correction, tip 포함")
    private Object detailedAnalysis;

    public static KopicReportDetailItemResponse from(KopicReport report) {
        return KopicReportDetailItemResponse.builder()
                .kopicReportId(report.getKopicReportId())
                .kopicSentenceId(report.getKopicSentence().getKopicSentenceId())
                .textKo(report.getKopicSentence().getTextKo())
                .accuracy(report.getAccuracy())
                .totalScore(report.getTotalScore())
                .detailedAnalysis(report.getDetailedAnalysis())
                .build();
    }
}
