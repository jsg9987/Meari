package com.ssafy.meari.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.domain.report.dto.response.*;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.global.common.CursorPageResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShadowingReportService {

    private final ShadowingReportRepository shadowingReportRepository;
    private final ObjectMapper objectMapper;


    /**
     * 쉐도잉 리포트 목록 조회 (커서 기반 페이징)
     */
    public CursorPageResponse<ShadowingReportListItemResponse> getShadowingReportList(
            Long memberId, java.time.LocalDateTime cursor, int size) {
        log.debug("쉐도잉 리포트 목록 조회: memberId={}, cursor={}, size={}", memberId, cursor, size);

        // size + 1개 조회해서 다음 페이지 존재 여부 확인
        List<ShadowingReport> reports;
        if (cursor == null) {
            // 첫 페이지일 경우 cursor 없이 조회
            reports = shadowingReportRepository.findFirstPageByMemberId(
                    memberId, ReportStatus.COMPLETED, PageRequest.of(0, size + 1));
        } else {
            // cursor가 존재할 경우, created_at 내림차순이므로 cursor 이전의 데이터 조회
            reports = shadowingReportRepository.findNextPageByMemberId(
                    memberId, cursor, ReportStatus.COMPLETED, PageRequest.of(0, size + 1));
        }

        // 페이지 사이즈보다 1 클 경우 다음 페이지 존재
        boolean hasNext = reports.size() > size;
        if (hasNext) {
            reports = reports.subList(0, size);
        }

        // DTO 변환
        List<ShadowingReportListItemResponse> contents = reports.stream()
                .map(ShadowingReportListItemResponse::from)
                .collect(Collectors.toList());

        // 다음 페이지가 존재할 경우, 리스트의 마지막 값으로 다음 커서 설정
        Long nextCursor = hasNext && !reports.isEmpty()
                ? reports.get(reports.size() - 1).getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;

        return CursorPageResponse.of(contents, nextCursor, hasNext);
    }


    /**
     * 쉐도잉 리포트 상세 조회 (조회 시 자동으로 is_read=true로 변경)
     */
    @Transactional
    public ShadowingReportDetailResponse getShadowingReportDetail(Long reportId, Long memberId) {
        log.debug("쉐도잉 리포트 상세 조회: reportId={}, memberId={}", reportId, memberId);

        ShadowingReport report = shadowingReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_SHADOWING_REPORT));

        // 본인의 리포트인지 확인
        if (!report.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 읽음 처리 (Dirty Checking)
        report.markAsRead();

        return toShadowingReportDetailResponse(report);
    }


    /**
     * ShadowingReport → ShadowingReportDetailResponse 변환
     */
    private ShadowingReportDetailResponse toShadowingReportDetailResponse(ShadowingReport report) {
        // JSONB 문자열을 DetailedAnalysis 객체로 파싱
        DetailedAnalysis detailedAnalysis = parseDetailedAnalysis(report.getDetailedAnalysis());

        return ShadowingReportDetailResponse.builder()
                .shadowingReportId(report.getShadowingReportId())
                .memberId(report.getMember().getMemberId())
                .memberNickname(report.getMember().getNickname())
                .roomId(report.getRoom().getRoomId())
                .roomTitle(report.getRoom().getTitle())
                .contentId(report.getContent().getContentId())
                .contentTitle(report.getContent().getTitle())
                .roleId(report.getRole().getRoleId())
                .roleName(report.getRole().getName())
                .accuracy(report.getAccuracy())
                .intonation(report.getIntonation())
                .detailedAnalysis(detailedAnalysis)
                .totalScore((report.getAccuracy() + report.getIntonation()) / 2 )
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    /**
     * 최근 5회 쉐도잉 연습 이력 조회
     */
    public List<ShadowingPracticeHistoryResponse> getRecentPracticeHistory(Long memberId) {
        log.debug("최근 5회 쉐도잉 연습 이력 조회: memberId={}", memberId);

        // Repository에서 최근 5개 조회 (COMPLETED 상태만)
        List<ShadowingReport> reports = shadowingReportRepository.findTop5CompletedByMemberId(
                memberId,
                ReportStatus.COMPLETED,
                PageRequest.of(0, 5)
        );

        log.debug("조회된 리포트 개수: {}", reports.size());

        // DTO로 변환하면서 idx 부여 (1부터 시작)
        List<ShadowingPracticeHistoryResponse> responses = new java.util.ArrayList<>();
        for (int i = 0; i < reports.size(); i++) {
            responses.add(ShadowingPracticeHistoryResponse.from(reports.get(i), i + 1));
        }

        log.debug("최근 5회 쉐도잉 연습 이력 조회 완료");
        return responses;
    }


    /**
     * JSONB 문자열을 DetailedAnalysis 객체로 파싱
     */
    private DetailedAnalysis parseDetailedAnalysis(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonString, DetailedAnalysis.class);
        } catch (JsonProcessingException e) {
            log.error("DetailedAnalysis 파싱 실패: {}", e.getMessage());
            // 파싱 실패 시 빈 객체 반환 (또는 예외를 던질 수도 있음)
            return DetailedAnalysis.builder()
                    .sentences(List.of())
                    .summary(DetailedAnalysis.Summary.builder()
                            .totalSentences(0)
                            .analyzedSentences(0)
                            .averageAccuracy(0)
                            .averageConfidence(0.0)
                            .build())
                    .build();
        }
    }

}
