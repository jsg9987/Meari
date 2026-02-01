package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "쉐도잉 리포트 상세 조회 응답 (JSONB 분석 결과 포함)")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ShadowingReportDetailResponse {

    @Schema(description = "쉐도잉 리포트 ID", example = "1001")
    private Long shadowingReportId;

    // CER을 이용한 정답문장과의 일치정도 점수
    @Schema(description = "내용 정확도", example = "85")
    private Integer accuracy;

    // MFCC를 이용한 파형 유사도 분석 점수
    @Schema(description = "억양 점수", example = "90")
    private Integer intonation;

    // TODO 추후 평가항목 검토하여 수정 (현재 발음 필드 누락상태)

    @Schema(description = "총 점수", example = "87")
    private Integer totalScore;

    @Schema(description = "상세 분석 결과 (JSONB) - 문장별 accuracy_detail, intonation_detail 포함")
    private Object detailedAnalysis;

    @Schema(description = "읽음 여부", example = "true")
    private Boolean isRead;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    public static ShadowingReportDetailResponse from(ShadowingReport report) {
        Integer totalScore = null;
        if (report.getAccuracy() != null && report.getIntonation() != null) {
            totalScore = (report.getAccuracy() + report.getIntonation()) / 2;
        }

        return ShadowingReportDetailResponse.builder()
                .shadowingReportId(report.getShadowingReportId())
                .accuracy(report.getAccuracy())
                .intonation(report.getIntonation())
                .totalScore(totalScore)
                .detailedAnalysis(report.getDetailedAnalysis())
                .isRead(report.getIsRead())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
