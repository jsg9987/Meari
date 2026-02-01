package com.ssafy.meari.domain.report.controller;

import com.ssafy.meari.domain.report.dto.response.ReportDetailResponse;
import com.ssafy.meari.domain.report.dto.response.ReportResponse;
import com.ssafy.meari.domain.report.service.ReportService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Report", description = "쉐도잉 분석 리포트 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "방별 리포트 조회", description = "특정 방의 모든 멤버 리포트를 조회합니다.")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByRoom(
            @Parameter(description = "방 ID") @PathVariable Long roomId
    ) {
        log.info("방 {} 리포트 조회", roomId);
        List<ReportResponse> reports = reportService.getReportsByRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "내 리포트 목록 조회", description = "로그인한 사용자의 모든 리포트를 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getMyReports(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("멤버 {} 리포트 조회", memberId);
        List<ReportResponse> reports = reportService.getReportsByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "특정 멤버 리포트 조회", description = "특정 멤버의 모든 리포트를 조회합니다.")
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByMember(
            @Parameter(description = "멤버 ID") @PathVariable Long memberId
    ) {
        log.info("멤버 {} 리포트 조회", memberId);
        List<ReportResponse> reports = reportService.getReportsByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "리포트 상세 조회", description = "리포트의 상세 정보를 조회합니다. 상세 분석 결과(JSON)를 포함합니다.")
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportDetailResponse>> getReportDetail(
            @Parameter(description = "리포트 ID") @PathVariable Long reportId
    ) {
        log.info("리포트 {} 상세 조회", reportId);
        ReportDetailResponse report = reportService.getReportDetail(reportId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
