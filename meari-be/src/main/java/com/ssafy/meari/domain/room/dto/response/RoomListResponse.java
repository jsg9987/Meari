package com.ssafy.meari.domain.room.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "방 목록 아이템 응답")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoomListResponse {

    @Schema(description = "방 ID", example = "1")
    private Long roomId;

    @Schema(description = "방 제목", example = "한국어 같이 배워요!")
    private String title;

    @Schema(description = "테마 이름", example = "비즈니스")
    private String themeName;

    @Schema(description = "현재 인원", example = "2")
    private Integer currentPeople;

    @Schema(description = "최대 인원", example = "4")
    private Integer maxPeople;

    @Schema(description = "방 상태", example = "WAITING")
    private RoomStatus status;

    @Schema(description = "비밀방 여부", example = "true")
    private Boolean hasPassword;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    public static RoomListResponse from(Room room, int currentPeople) {
        return RoomListResponse.builder()
                .roomId(room.getRoomId())
                .title(room.getTitle())
                .themeName(room.getTheme().getName())
                .currentPeople(currentPeople)
                .maxPeople(room.getMaxPeople())
                .status(room.getStatus())
                .hasPassword(room.hasPassword())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
