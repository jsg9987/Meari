package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "코픽 리포트 목록에 들어갈 응답요소")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KopicReportListResponse {

    @Schema(description = "코픽 리포트 ID", example = "1001")
    private Long kopicReportId;

    @Schema(description = "테마 썸네일 URL", example = "https://cdn.example.com/content/thumbnail/default.png")
    private String thumbnailUrl;

    @Schema(description = "테마", example = "news")
    private String theme;

    @Schema(description = "총 점수", example = "89")
    private Integer totalScore;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    public static KopicReportListResponse from(KopicTotalReport report) {
        return KopicReportListResponse.builder()
                .kopicReportId(report.getKopicTotalReportId())
                .thumbnailUrl(report.getTheme().getThemeUrl())
                .theme(report.getTheme().getName())
                .totalScore(report.getTotalScore())
                .isRead(report.getIsRead())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
