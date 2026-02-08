package com.ssafy.meari.domain.kopic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.domain.kopic.dto.request.KopicEvaluateRequest;
import com.ssafy.meari.domain.kopic.dto.request.KopicTotalReportCreateRequest;
import com.ssafy.meari.domain.kopic.dto.response.KopicEvaluateResponse;
import com.ssafy.meari.domain.kopic.dto.response.KopicReportResponse;
import com.ssafy.meari.domain.kopic.dto.response.KopicTotalReportCreateResponse;
import com.ssafy.meari.domain.kopic.dto.response.KopicTotalReportResponse;
import com.ssafy.meari.domain.kopic.service.KopicEvaluateService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "4. Kopic", description = "코픽 문장 및 평가 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/kopic")
@RequiredArgsConstructor
public class KopicEvaluateController {

    private final KopicEvaluateService kopicEvaluateService;
    private final ObjectMapper objectMapper;

    @Operation(
            summary = "코픽 통합 리포트 생성",
            description = "코픽 학습 시작 시 통합 리포트를 먼저 생성합니다. 반환된 ID를 이후 발화 분석 요청에 사용합니다."
    )
    @PostMapping("/total-report")
    public ResponseEntity<ApiResponse<KopicTotalReportCreateResponse>> createTotalReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody KopicTotalReportCreateRequest request
    ) {
        KopicTotalReportCreateResponse response = kopicEvaluateService.createTotalReport(
                userDetails.getMember(), request.getThemeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "코픽 발화 분석 요청",
            description = "음성 파일을 S3에 업로드 후 Gemini AI로 분석합니다. 비동기로 처리되며 202 Accepted를 즉시 반환합니다."
    )
    @PostMapping(value = "/evaluate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KopicEvaluateResponse>> evaluate(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestPart("request") String requestJson,
            @RequestPart("audio") MultipartFile audioFile
    ) {
        try {
            KopicEvaluateRequest request = objectMapper.readValue(requestJson, KopicEvaluateRequest.class);
            log.debug("코픽 발화 분석 요청: memberId={}, sentenceId={}, fileName={}",
                    userDetails.getMember().getMemberId(), request.getKopicSentenceId(), audioFile.getOriginalFilename());

            byte[] audioData = audioFile.getBytes();
            String contentType = audioFile.getContentType();
            KopicEvaluateResponse response = kopicEvaluateService.evaluate(userDetails.getMember(), request, audioData, contentType);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
        } catch (java.io.IOException e) {
            throw new RuntimeException("요청 처리 실패", e);
        }
    }

    @Operation(
            summary = "코픽 리포트 조회",
            description = "분석 결과를 조회합니다. PROCESSING 상태면 상태값만, COMPLETED면 전체 결과를 반환합니다."
    )
    @GetMapping("/report/{reportId}")
    public ResponseEntity<ApiResponse<KopicReportResponse>> getReport(
            @Parameter(description = "코픽 리포트 ID", required = true, example = "1001")
            @PathVariable Long reportId
    ) {
        KopicReportResponse response = kopicEvaluateService.getReport(reportId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "코픽 통합 리포트 조회",
            description = "5개 문장 분석이 모두 완료되면 통합 리포트를 반환합니다. PROCESSING 중이면 진행 상황을 반환합니다."
    )
    @GetMapping("/total-report/{totalReportId}")
    public ResponseEntity<ApiResponse<KopicTotalReportResponse>> getTotalReport(
            @Parameter(description = "코픽 통합 리포트 ID", required = true, example = "1")
            @PathVariable Long totalReportId
    ) {
        KopicTotalReportResponse response = kopicEvaluateService.getTotalReport(totalReportId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
