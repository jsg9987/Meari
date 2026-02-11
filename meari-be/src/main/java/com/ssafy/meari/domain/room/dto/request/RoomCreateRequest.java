package com.ssafy.meari.domain.room.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "방 생성 요청")
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoomCreateRequest {

    @Schema(description = "방 제목", example = "한국어 같이 배워요!")
    @NotBlank(message = "방 제목은 필수입니다.")
    @Size(max = 100, message = "방 제목은 100자 이내여야 합니다.")
    private String title;

    @Schema(description = "테마 ID", example = "1")
    @NotNull(message = "테마 ID는 필수입니다.")
    private Long themeId;

    @Schema(description = "최대 인원 (1~4명)", example = "4")
    @NotNull(message = "최대 인원은 필수입니다.")
    @Min(value = 1, message = "최소 1명 이상이어야 합니다.")
    @Max(value = 4, message = "최대 4명까지 가능합니다.")
    private Integer maxPeople;

    @Schema(description = "비밀번호 (선택)", example = "1234")
    @Size(max = 20, message = "비밀번호는 20자 이내여야 합니다.")
    private String password;
}
