package com.ssafy.meari.domain.content.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 역할(캐릭터) 목록 응답 DTO
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "역할(캐릭터) 정보")
public class RoleListResponse {

    @Schema(description = "역할 ID", example = "1")
    private Long roleId;

    @Schema(description = "콘텐츠 ID", example = "101")
    private Long contentId;

    @Schema(description = "역할 이름", example = "점원")
    private String name;
}
