package com.ssafy.meari.domain.room.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "동영상 선택 요청")
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ContentSelectRequest {

    @Schema(description = "콘텐츠 ID", example = "1")
    @NotNull(message = "콘텐츠 ID는 필수입니다.")
    private Long contentId;
}
