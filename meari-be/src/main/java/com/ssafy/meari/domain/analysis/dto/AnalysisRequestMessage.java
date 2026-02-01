package com.ssafy.meari.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "멤버별 발음 분석 요청 메시지 (RabbitMQ)")
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AnalysisRequestMessage {

    @Schema(description = "방 ID", example = "123")
    private Long roomId;

    @Schema(description = "라운드 번호", example = "1")
    private Integer round;

    @Schema(description = "컨텐츠 ID", example = "101")
    private Long contentId;

    @Schema(description = "멤버 ID", example = "1")
    private Long memberId;

    @Schema(description = "역할 ID", example = "1")
    private Long roleId;

    @Schema(description = "문장 목록 (이 멤버가 녹음한 모든 문장)")
    private List<SentenceAnalysisInfo> sentences;

    @Builder
    public AnalysisRequestMessage(Long roomId, Integer round, Long contentId,
                                  Long memberId, Long roleId,
                                  List<SentenceAnalysisInfo> sentences) {
        this.roomId = roomId;
        this.round = round;
        this.contentId = contentId;
        this.memberId = memberId;
        this.roleId = roleId;
        this.sentences = sentences;
    }
}
