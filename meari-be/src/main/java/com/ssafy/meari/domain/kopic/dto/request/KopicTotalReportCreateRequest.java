package com.ssafy.meari.domain.kopic.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "코픽 통합 리포트 생성 요청")
public class KopicTotalReportCreateRequest {

    @NotNull(message = "테마 ID는 필수입니다.")
    @Schema(description = "테마 ID", example = "1")
    private Long themeId;
}
