package com.ssafy.meari.domain.solo_practice.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "혼자연습 시작 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SoloPracticeStartRequest {

    @Schema(description = "콘텐츠 ID", example = "123")
    @NotNull(message = "콘텐츠 ID는 필수입니다.")
    private Long contentId;

    @Schema(description = "역할 ID", example = "456")
    @NotNull(message = "역할 ID는 필수입니다.")
    private Long roleId;
}
