package com.ssafy.meari.domain.solo_practice.controller;

import com.ssafy.meari.domain.solo_practice.dto.request.SoloPracticeStartRequest;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeStartResponse;
import com.ssafy.meari.domain.solo_practice.service.SoloPracticeService;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SoloPractice", description = "혼자연습 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/solo-practice")
@RequiredArgsConstructor
public class SoloPracticeController {

    private final SoloPracticeService soloPracticeService;

    @Operation(summary = "혼자연습 시작", description = "콘텐츠와 역할을 선택하여 혼자연습을 시작합니다.")
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SoloPracticeStartResponse>> startPractice(
            @Valid @RequestBody SoloPracticeStartRequest request
    ) {
        log.info("혼자연습 시작 요청: contentId={}, roleId={}", request.getContentId(), request.getRoleId());

        SoloPracticeStartResponse response = soloPracticeService.startPractice(
                request.getContentId(),
                request.getRoleId()
        );

        log.info("혼자연습 시작 완료: contentId={}, roleId={}", request.getContentId(), request.getRoleId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }
}
