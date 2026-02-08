package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "코픽 통합 리포트 목록에 들어갈 아이템 응답")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KopicTotalReportListItemResponse {

    @Schema(description = "코픽 통합 리포트 ID", example = "1001")
    private Long kopicTotalReportId;

    @Schema(description = "테마 ID", example = "1")
    private Long themeId;

    @Schema(description = "테마 이름", example = "공항")
    private String themeName;

    @Schema(description = "테마 이미지 URL", example = "https://cdn.example.com/theme/airport.png")
    private String themeUrl;

    @Schema(description = "총 점수", example = "85")
    private Integer totalScore;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    public static KopicTotalReportListItemResponse from(KopicTotalReport report) {
        return KopicTotalReportListItemResponse.builder()
                .kopicTotalReportId(report.getKopicTotalReportId())
                .themeId(report.getTheme().getThemeId())
                .themeName(report.getTheme().getName())
                .themeUrl(report.getTheme().getThemeUrl())
                .totalScore(report.getTotalScore())
                .isRead(report.getIsRead())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
