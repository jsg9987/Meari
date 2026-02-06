package com.ssafy.meari.domain.kopic.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "코픽 발화 분석 요청")
public class KopicEvaluateRequest {

    @NotNull(message = "통합 리포트 ID는 필수입니다.")
    @Schema(description = "코픽 통합 리포트 ID", example = "1")
    private Long kopicTotalReportId;

    @NotNull(message = "코픽 문장 ID는 필수입니다.")
    @Schema(description = "코픽 문장 ID", example = "501")
    private Long kopicSentenceId;

}
