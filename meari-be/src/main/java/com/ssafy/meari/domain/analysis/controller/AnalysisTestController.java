package com.ssafy.meari.domain.analysis.controller;

import com.ssafy.meari.domain.analysis.service.AnalysisService;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 분석 테스트용 컨트롤러
 * 프론트엔드 없이 직접 분석 요청을 테스트하기 위한 엔드포인트
 *
 * ⚠️ 개발/테스트 환경에서만 사용
 */
@Slf4j
@Tag(name = "Analysis Test", description = "분석 테스트 API (개발용)")
@RestController
@RequestMapping("/api/v1/analysis/test")
@RequiredArgsConstructor
public class AnalysisTestController {

    private final AnalysisService analysisService;

    /**
     * 분석 요청 테스트
     *
     * @param request 분석 요청 정보
     * @return 성공 메시지
     */
    @Operation(
        summary = "분석 요청 (테스트용)",
        description = "프론트엔드 없이 직접 분석을 요청합니다. " +
                      "Redis에 녹음 데이터가 이미 등록되어 있어야 합니다."
    )
    @PostMapping("/request")
    public ApiResponse<String> requestAnalysis(@RequestBody AnalysisTestRequest request) {
        log.info("=== 테스트 분석 요청 수신 ===");
        log.info("roomId={}, round={}, memberId={}",
                request.getRoomId(), request.getRound(), request.getMemberId());

        // 분석 요청 (비동기 실행)
        analysisService.requestMemberAnalysis(
                request.getRoomId(),
                request.getRound(),
                request.getMemberId()
        );

        String message = String.format(
                "분석 요청 성공 (비동기 실행 중): roomId=%d, round=%d, memberId=%d",
                request.getRoomId(), request.getRound(), request.getMemberId()
        );

        log.info("✅ {}", message);
        return ApiResponse.success(message);
    }

    @Getter
    @Setter
    public static class AnalysisTestRequest {
        private Long roomId;
        private Integer round;
        private Long memberId;
    }
}
