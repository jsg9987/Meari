package com.ssafy.meari.domain.kopic.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.KopicReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "코픽 발화 분석 요청 응답 (202 Accepted)")
public class KopicEvaluateResponse {

    @Schema(description = "코픽 리포트 ID", example = "1001")
    private Long kopicReportId;

    @Schema(description = "코픽 통합 리포트 ID", example = "1")
    private Long kopicTotalReportId;

    @Schema(description = "분석 상태", example = "PROCESSING")
    private String status;

    public static KopicEvaluateResponse from(KopicReport kopicReport) {
        return KopicEvaluateResponse.builder()
                .kopicReportId(kopicReport.getKopicReportId())
                .kopicTotalReportId(kopicReport.getKopicTotalReport().getKopicTotalReportId())
                .status(kopicReport.getStatus().name())
                .build();
    }
}
