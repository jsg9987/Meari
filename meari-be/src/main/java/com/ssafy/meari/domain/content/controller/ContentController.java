package com.ssafy.meari.domain.content.controller;

import com.ssafy.meari.domain.content.dto.response.ContentListResponse;
import com.ssafy.meari.domain.content.dto.response.QuizResponseDto;
import com.ssafy.meari.domain.content.dto.response.RoleListResponse;
import com.ssafy.meari.domain.content.dto.response.ThemeListResponse;
import com.ssafy.meari.domain.content.service.ContentService;
import com.ssafy.meari.domain.word.dto.response.WordResponseDto;
import com.ssafy.meari.domain.word.service.WordService;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 콘텐츠 조회 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
@Tag(name = "1. Content", description = "콘텐츠 조회 API")
public class ContentController {

    private final ContentService contentService;
    private final WordService wordService;

    /**
     * 테마 목록 조회
     */
    @GetMapping("/themes")
    @Operation(
            summary = "테마 목록 조회",
            description = "쉐도잉 학습에 사용할 수 있는 모든 테마 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<ThemeListResponse>>> getThemes() {
        log.info("테마 목록 조회 요청");

        List<ThemeListResponse> themes = contentService.getThemes();

        return ResponseEntity.ok(ApiResponse.success(themes));
    }

    /**
     * 특정 테마의 콘텐츠(영상) 목록 조회
     */
    @GetMapping("/{themeId}")
    @Operation(
            summary = "콘텐츠 목록 조회",
            description = "특정 테마에 속한 콘텐츠(영상) 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<ContentListResponse>>> getContentsByTheme(
            @Parameter(description = "테마 ID", example = "1")
            @PathVariable Long themeId
    ) {
        log.info("테마별 콘텐츠 목록 조회 요청: themeId={}", themeId);

        List<ContentListResponse> contents = contentService.getContentsByTheme(themeId);

        return ResponseEntity.ok(ApiResponse.success(contents));
    }

    /**
     * 특정 콘텐츠의 역할(캐릭터) 목록 조회
     */
    @GetMapping("/{contentId}/roles")
    @Operation(
            summary = "역할(캐릭터) 목록 조회",
            description = "특정 콘텐츠에 등장하는 역할(캐릭터) 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<RoleListResponse>>> getRolesByContent(
            @Parameter(description = "콘텐츠 ID", example = "101")
            @PathVariable Long contentId
    ) {
        log.info("콘텐츠별 역할 목록 조회 요청: contentId={}", contentId);

        List<RoleListResponse> roles = contentService.getRolesByContent(contentId);

        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    /**
     * 문장 순서 맞추기 퀴즈 조회
     */
    @GetMapping("/quiz")
    @Operation(
            summary = "문장 순서 맞추기 퀴즈",
            description = "랜덤 문장을 가져와 띄어쓰기 기준으로 단어를 쪼개고 셔플하여 반환합니다. 프론트에서 index 순서(0,1,2...)로 배치하면 정답입니다."
    )
    public ResponseEntity<ApiResponse<List<QuizResponseDto>>> getQuiz() {
        log.info("문장 순서 맞추기 퀴즈 조회 요청");

        List<QuizResponseDto> quiz = contentService.getQuiz();

        return ResponseEntity.ok(ApiResponse.success(quiz));
    }

    /**
     * 랜덤 단어 10개 조회
     */
    @GetMapping("/words/random")
    @Operation(
            summary = "랜덤 단어 10개 조회",
            description = "랜덤 단어 10개를 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<WordResponseDto>>> getRandomWords() {
        log.info("랜덤 단어 10개 조회 요청");

        List<WordResponseDto> randomWords = wordService.getRandomWords();

        return ResponseEntity.ok(ApiResponse.success(randomWords));
    }
}

