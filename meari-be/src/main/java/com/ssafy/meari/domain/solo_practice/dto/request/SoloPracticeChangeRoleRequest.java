package com.ssafy.meari.domain.solo_practice.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "혼자연습 역할 변경 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SoloPracticeChangeRoleRequest {

    @Schema(description = "변경할 역할 ID", example = "457")
    @NotNull(message = "역할 ID는 필수입니다.")
    private Long roleId;
}
