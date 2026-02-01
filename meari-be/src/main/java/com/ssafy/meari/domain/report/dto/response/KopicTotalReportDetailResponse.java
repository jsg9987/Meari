package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "코픽 통합 리포트 상세 조회 응답 (상태별로 다른 구조)")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KopicTotalReportDetailResponse {

    @Schema(description = "코픽 통합 리포트 ID", example = "7")
    private Long kopicTotalReportId;

    @Schema(description = "회원 ID (COMPLETED 상태에서만 반환)", example = "1")
    private Long memberId;

    @Schema(description = "평균 정확도 점수 (COMPLETED 상태에서만 반환)", example = "82")
    private Integer avgAccuracy;

    // TODO 추후 평가항목 검토하여 수정 (현재 발음 필드 누락상태)

    @Schema(description = "총 점수 (COMPLETED 상태에서만 반환)", example = "80")
    private Integer totalScore;

    @Schema(description = "평가 문장 수 (COMPLETED 상태에서만 반환)", example = "5")
    private Integer sentenceCount;

    @Schema(description = "상태 (PROCESSING, COMPLETED)", example = "COMPLETED")
    private String status;

    @Schema(description = "완료된 개별 리포트 개수 (PROCESSING 상태에서만 반환)", example = "3")
    private Integer completedCount;

    @Schema(description = "총 개별 리포트 개수 (PROCESSING 상태에서만 반환)", example = "5")
    private Integer totalCount;

    @Schema(description = "상세 분석 결과 배열 (COMPLETED 상태에서만 반환, kopic_sentence_id 오름차순 정렬)")
    private List<KopicReportDetailItemResponse> reportData;

    /**
     * PROCESSING 상태 리포트 응답 생성
     */
    public static KopicTotalReportDetailResponse ofProcessing(KopicTotalReport report, int completedCount, int totalCount) {
        return KopicTotalReportDetailResponse.builder()
                .kopicTotalReportId(report.getKopicTotalReportId())
                .status(report.getStatus().name())
                .completedCount(completedCount)
                .totalCount(totalCount)
                .build();
    }

    /**
     * COMPLETED 상태 리포트 응답 생성
     */
    public static KopicTotalReportDetailResponse ofCompleted(
            KopicTotalReport report,
            List<KopicReportDetailItemResponse> reportDataItems
    ) {
        return KopicTotalReportDetailResponse.builder()
                .kopicTotalReportId(report.getKopicTotalReportId())
                .memberId(report.getMember().getMemberId())
                .avgAccuracy(report.getAvgAccuracy())
                .totalScore(report.getTotalScore())
                .sentenceCount(report.getSentenceCount())
                .status(report.getStatus().name())
                .reportData(reportDataItems)
                .build();
    }
}
