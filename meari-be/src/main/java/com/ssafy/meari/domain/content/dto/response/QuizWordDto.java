package com.ssafy.meari.domain.content.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "퀴즈 단어 조각")
public class QuizWordDto {

    @Schema(description = "단어 텍스트", example = "어서오세요")
    private String text;

    @Schema(description = "원래 문장에서의 인덱스 (0부터 시작)", example = "0")
    private Integer index;
}
