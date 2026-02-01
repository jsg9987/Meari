package com.ssafy.meari.domain.kopic.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "코픽 세션 통합 리포트 응답")
public class KopicTotalReportResponse {

    @Schema(description = "통합 리포트 ID", example = "1")
    private Long kopicTotalReportId;

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "평균 정확도", example = "82")
    private Integer avgAccuracy;

    @Schema(description = "총점", example = "80")
    private Integer totalScore;

    @Schema(description = "문장 수", example = "5")
    private Integer sentenceCount;

    @Schema(description = "분석 상태", example = "COMPLETED")
    private String status;

    @Schema(description = "완료된 문장 수 (PROCESSING 시)", example = "3")
    private Long completedCount;

    @Schema(description = "전체 문장 수 (PROCESSING 시)", example = "5")
    private Long totalCount;

    @Schema(description = "문장별 상세 분석 결과 (kopic_sentence_id 오름차순)")
    private List<Object> reportData;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static KopicTotalReportResponse from(KopicTotalReport totalReport) {
        List<Object> reportDataList = null;
        if (totalReport.getReportData() != null) {
            try {
                reportDataList = objectMapper.readValue(totalReport.getReportData(), List.class);
            } catch (JsonProcessingException e) {
                reportDataList = List.of();
            }
        }

        return KopicTotalReportResponse.builder()
                .kopicTotalReportId(totalReport.getKopicTotalReportId())
                .memberId(totalReport.getMember().getMemberId())
                .avgAccuracy(totalReport.getAvgAccuracy())
                .totalScore(totalReport.getTotalScore())
                .sentenceCount(totalReport.getSentenceCount())
                .status(totalReport.getStatus().name())
                .reportData(reportDataList)
                .build();
    }

    public static KopicTotalReportResponse processingFrom(Long kopicTotalReportId, long totalCount, long completedCount) {
        return KopicTotalReportResponse.builder()
                .kopicTotalReportId(kopicTotalReportId)
                .status("PROCESSING")
                .totalCount(totalCount)
                .completedCount(completedCount)
                .build();
    }
}
