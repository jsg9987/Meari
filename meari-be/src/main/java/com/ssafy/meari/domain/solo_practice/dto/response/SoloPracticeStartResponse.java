package com.ssafy.meari.domain.solo_practice.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "혼자연습 시작 응답")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SoloPracticeStartResponse {

    @Schema(description = "콘텐츠 정보")
    private SoloPracticeContentResponse content;

    @Schema(description = "선택된 역할")
    private SoloPracticeRoleResponse selectedRole;

    @Schema(description = "이 콘텐츠의 모든 역할")
    private List<SoloPracticeRoleResponse> allRoles;

    @Schema(description = "모든 문장 정보")
    private List<SoloPracticeSentenceResponse> sentences;
}
