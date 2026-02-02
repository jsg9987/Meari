package com.ssafy.meari.domain.dashboard.controller;

import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;
import com.ssafy.meari.domain.dashboard.service.DashboardService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "대시보드 API")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me/daily-records")
    @Operation(summary = "일일학습 기록 조회", description = "GitHub 잔디 형식의 일일학습 기록을 조회합니다. weekly는 월요일~일요일, monthly는 1일~마지막날 기준입니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (period 값 오류)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<ApiResponse<DailyRecordsResponse>> getDailyRecords(
        @AuthenticationPrincipal UserDetailsImpl userDetails,
        @RequestParam
        @Parameter(description = "조회 기간 (weekly: 월요일~일요일, monthly: 1일~마지막날)", example = "weekly")
        String period,
        @RequestParam(required = false)
        @Parameter(description = "기준 날짜 (기본값: 오늘)", example = "2025-02-02")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate referenceDate
    ) {
        log.debug("일일학습 기록 조회 요청 - memberId: {}, period: {}",
            userDetails.getMember().getMemberId(), period);

        // 기준 날짜가 없으면 오늘 날짜 사용
        LocalDate date = referenceDate != null ? referenceDate : LocalDate.now();

        DailyRecordsResponse response = dashboardService.getDailyRecords(
            userDetails.getMember().getMemberId(), period, date);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
