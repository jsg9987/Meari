package com.ssafy.meari.domain.solo_practice.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.content.entity.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Schema(description = "혼자연습 콘텐츠 정보")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SoloPracticeContentResponse {

    @Schema(description = "콘텐츠 ID", example = "123")
    private Long contentId;

    @Schema(description = "콘텐츠 제목", example = "Restaurant Ordering")
    private String title;

    @Schema(description = "영상 URL")
    private String videoUrl;

    @Schema(description = "썸네일 URL")
    private String thumbnailUrl;

    @Schema(description = "전체 길이 (초)", example = "125.5")
    private BigDecimal totalDuration;

    public static SoloPracticeContentResponse from(Content content) {
        return SoloPracticeContentResponse.builder()
                .contentId(content.getContentId())
                .title(content.getTitle())
                .videoUrl(content.getVideoUrl())
                .thumbnailUrl(content.getThumbnailUrl())
                .totalDuration(content.getTotalDuration())
                .build();
    }
}
