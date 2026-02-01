package com.ssafy.meari.domain.report.controller;

import com.ssafy.meari.domain.report.dto.response.KopicReportListResponse;
import com.ssafy.meari.domain.report.dto.response.KopicTotalReportDetailResponse;
import com.ssafy.meari.domain.report.dto.response.ShadowingReportDetailResponse;
import com.ssafy.meari.domain.report.dto.response.ShadowingReportListResponse;
import com.ssafy.meari.domain.report.service.ReportService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.common.ApiResponse;
import com.ssafy.meari.global.common.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Tag(name = "Report", description = "리포트 조회 및 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "쉐도잉 리포트 목록 조회", description = "커서 기반 페이징으로 쉐도잉 리포트 목록을 조회합니다.")
    @GetMapping("/shadowing")
    public ResponseEntity<ApiResponse<CursorPageResponse<ShadowingReportListResponse>>> getShadowingReportList(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "커서 (이전 페이지 마지막 created_at의 ms 변환값)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기 (기본값: 10)")
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("쉐도잉 리포트 목록 조회 요청: memberId={}, cursor={}, size={}", memberId, cursor, size);

        // cursor (timestamp ms) → LocalDateTime 변환
        LocalDateTime cursorDateTime = cursor != null
                ? LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(cursor),
                        ZoneId.systemDefault())
                : null;

        CursorPageResponse<ShadowingReportListResponse> response =
                reportService.getShadowingReportList(memberId, cursorDateTime, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "코픽 리포트 목록 조회", description = "커서 기반 페이징으로 코픽 리포트 목록을 조회합니다.")
    @GetMapping("/kopic")
    public ResponseEntity<ApiResponse<CursorPageResponse<KopicReportListResponse>>> getKopicReportList(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "커서 (이전 페이지 마지막 created_at 타임스탬프 ms)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기 (기본값: 10)")
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("코픽 리포트 목록 조회 요청: memberId={}, cursor={}, size={}", memberId, cursor, size);

        // cursor (timestamp ms) → LocalDateTime 변환
        LocalDateTime cursorDateTime = cursor != null
                ? LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(cursor),
                        ZoneId.systemDefault())
                : null;

        CursorPageResponse<KopicReportListResponse> response =
                reportService.getKopicReportList(memberId, cursorDateTime, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "쉐도잉 리포트 상세 조회", description = "AI 분석이 완료된 개별 문장의 상세 결과를 조회합니다. (JSONB 포함, 조회 시 자동으로 읽음 처리)")
    @GetMapping("/shadowing/{reportId}")
    public ResponseEntity<ApiResponse<ShadowingReportDetailResponse>> getShadowingReportDetail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "리포트 ID") @PathVariable Long reportId
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("쉐도잉 리포트 상세 조회 요청: reportId={}, memberId={}", reportId, memberId);

        ShadowingReportDetailResponse response = reportService.getShadowingReportDetail(reportId, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "코픽 통합 리포트 상세 조회", description = "코픽 통합 리포트의 상태에 따라 다른 정보를 조회합니다. (PROCESSING: 진행률, COMPLETED: 상세 분석 결과)")
    @GetMapping("/kopic/total-report/{totalReportId}")
    public ResponseEntity<ApiResponse<KopicTotalReportDetailResponse>> getKopicTotalReportDetail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "코픽 통합 리포트 ID") @PathVariable Long totalReportId
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("코픽 통합 리포트 상세 조회 요청: totalReportId={}, memberId={}", totalReportId, memberId);

        KopicTotalReportDetailResponse response = reportService.getKopicTotalReportDetail(totalReportId, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
