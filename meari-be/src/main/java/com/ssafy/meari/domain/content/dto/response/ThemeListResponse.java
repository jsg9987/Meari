package com.ssafy.meari.domain.content.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 테마 목록 응답 DTO
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "테마 정보")
public class ThemeListResponse {

    @Schema(description = "테마 ID", example = "1")
    private Long themeId;

    @Schema(description = "테마 이름", example = "식당/카페")
    private String name;

    @Schema(description = "테마 설명", example = "식당이나 카페에서 사용하는 기본 회화")
    private String description;

    @Schema(description = "테마 이미지 URL", example = "https://cdn.../theme/cafe.jpg")
    private String themeUrl;
}
