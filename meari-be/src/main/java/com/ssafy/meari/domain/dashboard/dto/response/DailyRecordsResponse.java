package com.ssafy.meari.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "일일 학습 기록 목록 응답")
public class DailyRecordsResponse {

    @Schema(description = "조회 시작 날짜", example = "2025-01-27")
    private String startDate;

    @Schema(description = "조회 종료 날짜", example = "2025-02-02")
    private String endDate;

    @Schema(description = "학습 기록 목록 (완료한 날짜만 포함)")
    private List<ActivityResponse> activities;
}
