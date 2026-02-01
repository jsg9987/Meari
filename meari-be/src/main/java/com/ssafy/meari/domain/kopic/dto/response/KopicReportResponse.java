package com.ssafy.meari.domain.kopic.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.KopicReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "코픽 리포트 조회 응답")
public class KopicReportResponse {

    @Schema(description = "코픽 리포트 ID", example = "1001")
    private Long kopicReportId;

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "코픽 문장 ID", example = "501")
    private Long sentenceId;

    @Schema(description = "한국어 원문", example = "오늘 점심 메뉴는 뭐예요?")
    private String textKo;

    @Schema(description = "정확도 점수", example = "85")
    private Integer accuracy;

    @Schema(description = "분석 상태", example = "COMPLETED")
    private String status;

    @Schema(description = "상세 분석 결과")
    private Map<String, Object> detailedAnalysis;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static KopicReportResponse from(KopicReport report) {
        Map<String, Object> analysisMap = null;
        if (report.getDetailedAnalysis() != null) {
            try {
                analysisMap = objectMapper.readValue(report.getDetailedAnalysis(), Map.class);
            } catch (JsonProcessingException e) {
                analysisMap = Map.of("raw", report.getDetailedAnalysis());
            }
        }

        return KopicReportResponse.builder()
                .kopicReportId(report.getKopicReportId())
                .memberId(report.getMember().getMemberId())
                .sentenceId(report.getKopicSentence().getKopicSentenceId())
                .textKo(report.getKopicSentence().getTextKo())
                .accuracy(report.getAccuracy())
                .status(report.getStatus().name())
                .detailedAnalysis(analysisMap)
                .build();
    }

    public static KopicReportResponse processingFrom(KopicReport report) {
        return KopicReportResponse.builder()
                .kopicReportId(report.getKopicReportId())
                .status(report.getStatus().name())
                .build();
    }
}
