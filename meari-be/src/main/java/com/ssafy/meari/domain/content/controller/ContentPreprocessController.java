package com.ssafy.meari.domain.content.controller;

import com.ssafy.meari.domain.content.service.ContentPreprocessService;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "9. Admin", description = "관리자 전용 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/content")
@RequiredArgsConstructor
public class ContentPreprocessController {

    private final ContentPreprocessService contentPreprocessService;

    @Operation(
            summary = "Content 오디오 전처리",
            description = "동영상을 문장별로 잘라서 S3에 업로드하고 DB에 저장합니다."
    )
    @PostMapping("/{contentId}/preprocess-audio")
    public ResponseEntity<ApiResponse<String>> preprocessAudio(
            @PathVariable Long contentId
    ) {
        log.info("Content {} 오디오 전처리 요청", contentId);

        contentPreprocessService.preprocessContentAudio(contentId);

        return ResponseEntity.ok(
                ApiResponse.success("Content " + contentId + " 오디오 전처리 완료")
        );
    }
}
