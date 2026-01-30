package com.ssafy.meari.domain.s3.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 동영상 URL 응답 DTO
 */
@Schema(description = "동영상 URL 응답")
@Getter
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoUrlResponse {

    @Schema(description = "동영상 Presigned URL", example = "https://meari-bucket.s3.ap-northeast-2.amazonaws.com/videos/1/video.mp4?...")
    private String videoUrl;

    @Schema(description = "URL 만료 시간 (초)", example = "3600")
    private Long expiresIn;

    public static VideoUrlResponse of(String videoUrl, Long expiresIn) {
        return VideoUrlResponse.builder()
                .videoUrl(videoUrl)
                .expiresIn(expiresIn)
                .build();
    }
}
