package com.ssafy.meari.domain.report.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.report.entity.ReportStatus;
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

    @Schema(description = "멤버 ID", example = "1")
    private final Long memberId;

    @Schema(description = "멤버 닉네임", example = "홍길동")
    private final String memberNickname;

    @Schema(description = "방 ID", example = "123")
    private final Long roomId;

    @Schema(description = "방 제목", example = "한국어 연습방")
    private final String roomTitle;

    @Schema(description = "컨텐츠 ID", example = "101")
    private final Long contentId;

    @Schema(description = "컨텐츠 제목", example = "일상 대화")
    private final String contentTitle;

    // TODO 추후 평가항목 검토하여 수정 (현재 발음 필드 누락상태)

    @Schema(description = "역할 ID", example = "1")
    private final Long roleId;

    @Schema(description = "역할 이름", example = "손님")
    private final String roleName;

    // CER을 이용한 정답문장과의 일치정도 점수
    @Schema(description = "정확도 점수 (0-100)", example = "85")
    private final Integer accuracy;

    // MFCC를 이용한 파형 유사도 분석 점수
    @Schema(description = "억양 점수 (0-100)", example = "90")
    private final Integer intonation;

    @Schema(description = "총 점수", example = "87")
    private Integer totalScore;

    @Schema(description = "상세 분석 결과")
    private final DetailedAnalysis detailedAnalysis;

    @Schema(description = "분석 상태", example = "COMPLETED")
    private final ReportStatus status;

    @Schema(description = "생성일시", example = "2025-01-30T10:00:00")
    private final LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-30T10:30:00")
    private final LocalDateTime updatedAt;



    @Builder
    public ShadowingReportDetailResponse(Long shadowingReportId, Long memberId, String memberNickname,
                                         Long roomId, String roomTitle,
                                         Long contentId, String contentTitle,
                                         Long roleId, String roleName,
                                         Integer accuracy, Integer intonation,
                                         Integer totalScore,
                                         DetailedAnalysis detailedAnalysis, ReportStatus status,
                                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.shadowingReportId = shadowingReportId;
        this.memberId = memberId;
        this.memberNickname = memberNickname;
        this.roomId = roomId;
        this.roomTitle = roomTitle;
        this.contentId = contentId;
        this.contentTitle = contentTitle;
        this.roleId = roleId;
        this.roleName = roleName;
        this.accuracy = accuracy;
        this.intonation = intonation;
        this.totalScore = totalScore;
        this.detailedAnalysis = detailedAnalysis;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
