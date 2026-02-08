package com.ssafy.meari.domain.solo_practice.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.content.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "혼자연습 역할 정보")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SoloPracticeRoleResponse {

    @Schema(description = "역할 ID", example = "456")
    private Long roleId;

    @Schema(description = "역할 이름", example = "Customer")
    private String name;

    public static SoloPracticeRoleResponse from(Role role) {
        return SoloPracticeRoleResponse.builder()
                .roleId(role.getRoleId())
                .name(role.getName())
                .build();
    }
}
