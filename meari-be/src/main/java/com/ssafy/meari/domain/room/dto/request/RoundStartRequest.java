package com.ssafy.meari.domain.room.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Round 시작 요청")
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoundStartRequest {

    @Schema(description = "라운드 번호 (1 또는 2)", example = "1", required = true)
    @NotNull(message = "라운드 번호는 필수입니다.")
    @Min(value = 1, message = "라운드는 1 이상이어야 합니다.")
    @Max(value = 2, message = "라운드는 2 이하여야 합니다.")
    private Integer round;
}
