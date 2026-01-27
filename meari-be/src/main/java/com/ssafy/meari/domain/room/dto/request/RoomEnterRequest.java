package com.ssafy.meari.domain.room.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "방 입장 요청")
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoomEnterRequest {

    @Schema(description = "비밀번호 (비밀방인 경우)", example = "1234")
    @Size(max = 20, message = "비밀번호는 20자 이내여야 합니다.")
    private String password;
}
