package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "쉐도잉 리포트 목록 아이템 응답")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ShadowingReportListItemResponse {

    @Schema(description = "쉐도잉 리포트 ID", example = "1001")
    private Long shadowingReportId;

    @Schema(description = "콘텐츠 썸네일 URL", example = "https://cdn.example.com/content/thumbnail/default.png")
    private String thumbnailUrl;

    @Schema(description = "방 제목", example = "초보만")
    private String roomTitle;

    @Schema(description = "콘텐츠 제목", example = "Grocery inflation")
    private String contentTitle;

    @Schema(description = "총 점수", example = "89")
    private Integer totalScore;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    public static ShadowingReportListItemResponse from(ShadowingReport report) {

        // TODO totalScore 계산로직 수정 (현재는 임의로 단순하게 절반씩 반영하였음)
        // TODO 추후 평가항목 검토하여 수정 (현재 발음 필드 누락상태)
        Integer totalScore = null;
        if (report.getAccuracy() != null && report.getIntonation() != null) {
            totalScore = (report.getAccuracy() + report.getIntonation()) / 2;
        }

        return ShadowingReportListItemResponse.builder()
                .shadowingReportId(report.getShadowingReportId())
                .thumbnailUrl(report.getContent().getThumbnailUrl())
                .roomTitle(report.getRoom().getTitle())
                .contentTitle(report.getContent().getTitle())
                .totalScore(totalScore)
                .isRead(report.getIsRead())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
