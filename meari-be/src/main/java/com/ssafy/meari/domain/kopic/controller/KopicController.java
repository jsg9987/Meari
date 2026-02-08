package com.ssafy.meari.domain.kopic.controller;

import com.ssafy.meari.domain.kopic.dto.response.KopicSentenceResponse;
import com.ssafy.meari.domain.kopic.service.KopicService;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "4. Kopic", description = "코픽 문장 및 평가 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
public class KopicController {

    private final KopicService kopicService;

    @Operation(
            summary = "코픽 문장 랜덤 조회",
            description = "테마별 코픽 문장 5개를 랜덤으로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<KopicSentenceResponse>>> getRandomKopicSentences(
            @Parameter(description = "테마 ID", required = true, example = "1")
            @RequestParam("theme_id") Long themeId
    ) {
        log.debug("코픽 문장 랜덤 조회 요청: themeId={}", themeId);
        List<KopicSentenceResponse> responses = kopicService.getRandomKopicSentences(themeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
