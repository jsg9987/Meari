package com.ssafy.meari.domain.s3.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 녹음 파일 업로드용 Presigned URL 요청 DTO
 */
@Schema(description = "Presigned URL 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PresignedUrlRequest {

    @Schema(description = "방 ID", example = "1")
    @NotNull(message = "방 ID는 필수입니다.")
    private Long roomId;

    @Schema(description = "라운드 번호 (1 또는 2)", example = "1")
    @NotNull(message = "라운드 번호는 필수입니다.")
    private Integer round;

    @Schema(description = "멤버 ID", example = "10")
    @NotNull(message = "멤버 ID는 필수입니다.")
    private Long memberId;

    @Schema(description = "문장 ID", example = "123")
    @NotNull(message = "문장 ID는 필수입니다.")
    private Long sentenceId;
}
