package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 내부적으로 사용하는 라운드별 상세 응답인지 확인 필요
@Schema(description = "쉐도잉 리포트 상세 응답")
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ReportDetailResponse {

    @Schema(description = "리포트 ID", example = "1")
    private final Long shadowingReportId;

    @Schema(description = "멤버 ID", example = "1")
    private final Long memberId;

    @Schema(description = "멤버 닉네임", example = "홍길동")
    private final String memberNickname;

    @Schema(description = "방 ID", example = "123")
    private final Long roomId;

    @Schema(description = "방 제목", example = "한국어 연습방")
    private final String roomTitle;

    @Schema(description = "라운드", example = "1")
    private final Integer round;

    @Schema(description = "컨텐츠 ID", example = "101")
    private final Long contentId;

    @Schema(description = "컨텐츠 제목", example = "일상 대화")
    private final String contentTitle;

    @Schema(description = "역할 ID", example = "1")
    private final Long roleId;

    @Schema(description = "역할 이름", example = "손님")
    private final String roleName;

    @Schema(description = "정확도 점수 (0-100)", example = "85")
    private final Integer accuracy;

    @Schema(description = "억양 점수 (0-100)", example = "90")
    private final Integer intonation;

    @Schema(description = "상세 분석 결과")
    private final DetailedAnalysis detailedAnalysis;

    @Schema(description = "분석 상태", example = "COMPLETED")
    private final ReportStatus status;

    @Schema(description = "생성일시", example = "2025-01-30T10:00:00")
    private final LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-30T10:30:00")
    private final LocalDateTime updatedAt;

    @Builder
    public ReportDetailResponse(Long shadowingReportId, Long memberId, String memberNickname,
                                Long roomId, String roomTitle, Integer round,
                                Long contentId, String contentTitle,
                                Long roleId, String roleName,
                                Integer accuracy, Integer intonation,
                                DetailedAnalysis detailedAnalysis, ReportStatus status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.shadowingReportId = shadowingReportId;
        this.memberId = memberId;
        this.memberNickname = memberNickname;
        this.roomId = roomId;
        this.roomTitle = roomTitle;
        this.round = round;
        this.contentId = contentId;
        this.contentTitle = contentTitle;
        this.roleId = roleId;
        this.roleName = roleName;
        this.accuracy = accuracy;
        this.intonation = intonation;
        this.detailedAnalysis = detailedAnalysis;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
