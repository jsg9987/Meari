package com.ssafy.meari.domain.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "사용자 활동 내역 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserActivityResponse {

    @Schema(description = "활동 타입", example = "DAILY", allowableValues = {"DAILY", "SHADOWING", "KOPIC"})
    private String activityType;

    @Schema(description = "테마 (SHADOWING/KOPIC)", example = "공공장소")
    private String theme;

    @Schema(description = "콘텐츠 내용 (SHADOWING만)", example = "카페에서 커피 주문하기")
    private String content;

    @Schema(description = "활동 제목", example = "일일학습 완료했습니다!")
    private String title;

    @Schema(description = "상태", example = "COMPLETED")
    private String status;

    @Schema(description = "생성 시간", example = "2026-02-01T20:10:00")
    private LocalDateTime createdAt;
}
