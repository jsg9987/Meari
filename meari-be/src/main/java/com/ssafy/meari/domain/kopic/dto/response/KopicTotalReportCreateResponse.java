package com.ssafy.meari.domain.kopic.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "코픽 통합 리포트 생성 응답")
public class KopicTotalReportCreateResponse {

    @Schema(description = "코픽 통합 리포트 ID", example = "1")
    private Long kopicTotalReportId;

    public static KopicTotalReportCreateResponse from(KopicTotalReport totalReport) {
        return KopicTotalReportCreateResponse.builder()
                .kopicTotalReportId(totalReport.getKopicTotalReportId())
                .build();
    }
}
