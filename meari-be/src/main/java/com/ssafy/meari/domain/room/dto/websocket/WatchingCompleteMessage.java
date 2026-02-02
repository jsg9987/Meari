package com.ssafy.meari.domain.room.dto.websocket;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Client → Server: 영상 시청 완료 메시지
 * Destination: /app/room/{roomId}/watching/complete
 */
@Getter
@Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WatchingCompleteMessage {

    private Long memberId;
}
