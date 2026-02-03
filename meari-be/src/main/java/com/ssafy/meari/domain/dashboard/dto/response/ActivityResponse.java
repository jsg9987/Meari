package com.ssafy.meari.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "일일 학습 기록 응답")
public class ActivityResponse {

    @Schema(description = "기록 날짜", example = "2025-02-02")
    private String date;

    @Schema(description = "완료한 일일학습 개수", example = "2")
    private Integer completed_count;
}
