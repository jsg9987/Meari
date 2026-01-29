package com.ssafy.meari.domain.room.dto.websocket;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

/**
 * ROUND_START 메시지 내부 DTO - 문장 단위 정보
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SentenceSegmentInfo {

    private Long sentenceId;
    private Integer sequence;
    private Double startTime;
    private Double endTime;
    private String textKo;
    private String textVn;
}
