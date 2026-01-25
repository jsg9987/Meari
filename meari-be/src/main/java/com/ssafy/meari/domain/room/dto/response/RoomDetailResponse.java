package com.ssafy.meari.domain.room.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "방 상세 응답 (참여자 목록 포함)")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoomDetailResponse {

    @Schema(description = "방 ID", example = "1")
    private Long roomId;

    @Schema(description = "방장 ID", example = "1")
    private Long ownerId;

    @Schema(description = "테마 ID", example = "1")
    private Long themeId;

    @Schema(description = "테마 이름", example = "비즈니스")
    private String themeName;

    @Schema(description = "방 제목", example = "한국어 같이 배워요!")
    private String title;

    @Schema(description = "최대 인원", example = "4")
    private Integer maxPeople;

    @Schema(description = "방 상태", example = "WAITING")
    private RoomStatus status;

    @Schema(description = "비밀방 여부", example = "true")
    private Boolean hasPassword;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "참여자 목록")
    private List<RoomMemberResponse> members;

    public static RoomDetailResponse from(Room room, List<RoomMemberResponse> members) {
        return RoomDetailResponse.builder()
                .roomId(room.getRoomId())
                .ownerId(room.getOwner().getMemberId())
                .themeId(room.getTheme().getThemeId())
                .themeName(room.getTheme().getName())
                .title(room.getTitle())
                .maxPeople(room.getMaxPeople())
                .status(room.getStatus())
                .hasPassword(room.hasPassword())
                .createdAt(room.getCreatedAt())
                .members(members)
                .build();
    }
}
