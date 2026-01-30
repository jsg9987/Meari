package com.ssafy.meari.domain.room.dto.websocket;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Client → Server: 문장별 녹음 완료 메시지
 * Destination: /app/room/{roomId}/recording/complete
 */
@Getter
@Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RecordingCompleteMessage {

    private Long memberId;
    private Long sentenceId;
    private String audioUrl;
}
