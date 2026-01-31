package com.ssafy.meari.domain.room.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "역할 확정 요청")
public class RoleConfirmRequest {

    @Valid
    @NotEmpty(message = "역할 목록은 비어있을 수 없습니다.")
    @Schema(description = "확정된 역할 목록", example = "[{\"member_id\": 102, \"role_id\": 1}]")
    private List<RoleAssignment> roles;

    @Getter
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "멤버별 역할 할당")
    public static class RoleAssignment {

        @Schema(description = "멤버 ID", example = "102")
        private Long memberId;

        @Schema(description = "역할 ID", example = "1")
        private Long roleId;
    }
}
