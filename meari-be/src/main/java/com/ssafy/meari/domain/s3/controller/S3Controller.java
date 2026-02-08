package com.ssafy.meari.domain.s3.controller;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.s3.dto.request.PresignedUrlRequest;
import com.ssafy.meari.domain.s3.dto.response.PresignedUrlResponse;
import com.ssafy.meari.domain.s3.dto.response.VideoUrlResponse;
import com.ssafy.meari.global.common.ApiResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import com.ssafy.meari.global.util.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * S3 파일 관리 컨트롤러
 */
@Tag(name = "7. S3", description = "파일 업로드/다운로드 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;
    private final ContentRepository contentRepository;

    @Value("${cloud.aws.presigned-url.video-expiration}")
    private long videoExpirationSeconds;

    @Value("${cloud.aws.presigned-url.upload-expiration}")
    private long uploadExpirationSeconds;

    /**
     * 녹음 파일 업로드용 Presigned URL 발급
     */
    @Operation(
            summary = "녹음 파일 업로드용 Presigned URL 발급",
            description = "클라이언트가 S3에 직접 녹음 파일을 업로드할 수 있는 Presigned URL을 생성합니다."
    )
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        log.info("Presigned URL 발급 요청: roomId={}, round={}, memberId={}, sentenceId={}",
                request.getRoomId(), request.getRound(), request.getMemberId(), request.getSentenceId());

        // S3 키 생성
        String s3Key = s3Service.generateRecordingKey(
                request.getRoomId(),
                request.getRound(),
                request.getMemberId(),
                request.getSentenceId()
        );

        // Presigned URL 생성
        String uploadUrl = s3Service.generatePresignedUrlForUpload(s3Key);

        PresignedUrlResponse response = PresignedUrlResponse.of(uploadUrl, s3Key, uploadExpirationSeconds);

        log.info("Presigned URL 발급 완료: s3Key={}", s3Key);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 동영상 시청용 Presigned URL 발급
     */
    @Operation(
            summary = "동영상 시청용 Presigned URL 발급",
            description = "콘텐츠 ID로 S3에 저장된 동영상의 Presigned URL을 생성합니다."
    )
    @GetMapping("/contents/{contentId}/video-url")
    public ResponseEntity<ApiResponse<VideoUrlResponse>> getVideoUrl(
            @PathVariable Long contentId
    ) {
        log.info("동영상 URL 요청: contentId={}", contentId);

        // Content 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        // videoUrl을 S3 키로 사용하여 Presigned URL 생성
        String presignedUrl = s3Service.generatePresignedUrlForVideo(content.getVideoUrl());

        VideoUrlResponse response = VideoUrlResponse.of(presignedUrl, videoExpirationSeconds);

        log.info("동영상 URL 발급 완료: contentId={}", contentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
