package com.ssafy.meari.domain.solo_practice.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.content.entity.Sentence;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "혼자연습 문장 정보")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SoloPracticeSentenceResponse {

    @Schema(description = "문장 ID", example = "1")
    private Long sentenceId;

    @Schema(description = "순서 (0부터 시작)", example = "0")
    private Integer sequence;

    @Schema(description = "시작 시간 (초)", example = "0.0")
    private Double startTime;

    @Schema(description = "종료 시간 (초)", example = "2.5")
    private Double endTime;

    @Schema(description = "한국어 텍스트", example = "안녕하세요")
    private String textKo;

    @Schema(description = "베트남어 텍스트", example = "Xin chào")
    private String textVn;

    @Schema(description = "역할 ID", example = "456")
    private Long roleId;

    @Schema(description = "역할 이름", example = "Customer")
    private String roleName;

    public static SoloPracticeSentenceResponse from(Sentence sentence) {
        return SoloPracticeSentenceResponse.builder()
                .sentenceId(sentence.getSentenceId())
                .sequence(sentence.getSequence())
                .startTime(sentence.getStartTime().doubleValue())
                .endTime(sentence.getEndTime().doubleValue())
                .textKo(sentence.getTextKo())
                .textVn(sentence.getTextVn())
                .roleId(sentence.getRole().getRoleId())
                .roleName(sentence.getRole().getName())
                .build();
    }
}
