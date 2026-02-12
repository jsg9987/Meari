package com.ssafy.meari.domain.room.dto.websocket;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * ROUND_START 메시지 내부 DTO - 멤버 단위 세그먼트
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MemberSegmentInfo {

    private Long memberId;
    private Long roleId;
    private String roleName;
    private List<SentenceSegmentInfo> sentences;
}
