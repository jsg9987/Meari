package com.ssafy.meari.domain.s3.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Presigned URL 응답 DTO
 */
@Schema(description = "Presigned URL 응답")
@Getter
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PresignedUrlResponse {

    @Schema(description = "업로드용 Presigned URL", example = "https://meari-bucket.s3.ap-northeast-2.amazonaws.com/recordings/1/round1/10/123.wav?...")
    private String uploadUrl;

    @Schema(description = "S3 객체 키", example = "recordings/1/round1/10/123.wav")
    private String s3Key;

    @Schema(description = "URL 만료 시간 (초)", example = "900")
    private Long expiresIn;

    public static PresignedUrlResponse of(String uploadUrl, String s3Key, Long expiresIn) {
        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .s3Key(s3Key)
                .expiresIn(expiresIn)
                .build();
    }
}
