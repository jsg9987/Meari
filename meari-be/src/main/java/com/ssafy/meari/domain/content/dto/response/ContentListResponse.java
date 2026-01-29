package com.ssafy.meari.domain.content.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 콘텐츠(영상) 목록 응답 DTO
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "콘텐츠 정보")
public class ContentListResponse {

    @Schema(description = "콘텐츠 ID", example = "101")
    private Long contentId;

    @Schema(description = "콘텐츠 제목", example = "카페에서 아메리카노 주문하기")
    private String title;

    @Schema(description = "썸네일 이미지 URL", example = "https://cdn.../thumb/cafe1.jpg")
    private String thumbnailUrl;

    @Schema(description = "최대 참여 인원", example = "2")
    private Integer maxPeople;
}
