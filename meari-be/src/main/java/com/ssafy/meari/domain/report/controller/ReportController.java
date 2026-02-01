package com.ssafy.meari.domain.report.controller;

import com.ssafy.meari.domain.report.dto.response.ReportResponse;
import com.ssafy.meari.domain.report.dto.response.ShadowingReportDetailResponse;
import com.ssafy.meari.domain.report.dto.response.ShadowingReportListItemResponse;
import com.ssafy.meari.domain.report.service.ShadowingReportService;
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
import java.util.List;

@Tag(name = "Report", description = "쉐도잉 분석 리포트 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ShadowingReportService shadowingReportService;

    @Operation(summary = "쉐도잉 리포트 목록 조회", description = "커서 기반 페이징으로 쉐도잉 리포트 목록을 조회합니다.")
    @GetMapping("/shadowing")
    public ResponseEntity<ApiResponse<CursorPageResponse<ShadowingReportListItemResponse>>> getShadowingReportListWithPagination(
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

        CursorPageResponse<ShadowingReportListItemResponse> response =
                shadowingReportService.getShadowingReportList(memberId, cursorDateTime, size);

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

        ShadowingReportDetailResponse response = shadowingReportService.getShadowingReportDetail(reportId, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "방별 리포트 조회", description = "특정 방의 모든 멤버 리포트를 조회합니다.")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByRoom(
            @Parameter(description = "방 ID") @PathVariable Long roomId
    ) {
        log.info("방 {} 리포트 조회", roomId);
        List<ReportResponse> reports = shadowingReportService.getReportsByRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "내 리포트 목록 조회", description = "로그인한 사용자의 모든 리포트를 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getMyReports(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("멤버 {} 리포트 조회", memberId);
        List<ReportResponse> reports = shadowingReportService.getReportsByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "특정 멤버 리포트 조회", description = "특정 멤버의 모든 리포트를 조회합니다.")
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByMember(
            @Parameter(description = "멤버 ID") @PathVariable Long memberId
    ) {
        log.info("멤버 {} 리포트 조회", memberId);
        List<ReportResponse> reports = shadowingReportService.getReportsByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }
}
