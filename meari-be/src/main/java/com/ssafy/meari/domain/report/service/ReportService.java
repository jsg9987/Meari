package com.ssafy.meari.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.domain.report.dto.response.DetailedAnalysis;
import com.ssafy.meari.domain.report.dto.response.ReportDetailResponse;
import com.ssafy.meari.domain.report.dto.response.ReportResponse;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ShadowingReportRepository shadowingReportRepository;
    private final ObjectMapper objectMapper;

    /**
     * 방 ID로 리포트 목록 조회
     */
    public List<ReportResponse> getReportsByRoom(Long roomId) {
        log.debug("방 {} 리포트 목록 조회", roomId);

        List<ShadowingReport> reports = shadowingReportRepository.findAll().stream()
                .filter(r -> r.getRoom().getRoomId().equals(roomId))
                .collect(Collectors.toList());

        return reports.stream()
                .map(this::toReportResponse)
                .collect(Collectors.toList());
    }

    /**
     * 멤버 ID로 리포트 목록 조회
     */
    public List<ReportResponse> getReportsByMember(Long memberId) {
        log.debug("멤버 {} 리포트 목록 조회", memberId);

        List<ShadowingReport> reports = shadowingReportRepository.findAll().stream()
                .filter(r -> r.getMember().getMemberId().equals(memberId))
                .collect(Collectors.toList());

        return reports.stream()
                .map(this::toReportResponse)
                .collect(Collectors.toList());
    }

    /**
     * 리포트 상세 조회
     */
    public ReportDetailResponse getReportDetail(Long reportId) {
        log.debug("리포트 {} 상세 조회", reportId);

        ShadowingReport report = shadowingReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_REPORT));

        return toReportDetailResponse(report);
    }

    /**
     * ShadowingReport → ReportResponse 변환
     */
    private ReportResponse toReportResponse(ShadowingReport report) {
        return ReportResponse.builder()
                .shadowingReportId(report.getShadowingReportId())
                .memberId(report.getMember().getMemberId())
                .memberNickname(report.getMember().getNickname())
                .roomId(report.getRoom().getRoomId())
                .roomTitle(report.getRoom().getTitle())
                .round(report.getRound())
                .contentId(report.getContent().getContentId())
                .contentTitle(report.getContent().getTitle())
                .roleId(report.getRole().getRoleId())
                .roleName(report.getRole().getName())
                .accuracy(report.getAccuracy())
                .intonation(report.getIntonation())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    /**
     * ShadowingReport → ReportDetailResponse 변환
     */
    private ReportDetailResponse toReportDetailResponse(ShadowingReport report) {
        // JSONB 문자열을 DetailedAnalysis 객체로 파싱
        DetailedAnalysis detailedAnalysis = parseDetailedAnalysis(report.getDetailedAnalysis());

        return ReportDetailResponse.builder()
                .shadowingReportId(report.getShadowingReportId())
                .memberId(report.getMember().getMemberId())
                .memberNickname(report.getMember().getNickname())
                .roomId(report.getRoom().getRoomId())
                .roomTitle(report.getRoom().getTitle())
                .round(report.getRound())
                .contentId(report.getContent().getContentId())
                .contentTitle(report.getContent().getTitle())
                .roleId(report.getRole().getRoleId())
                .roleName(report.getRole().getName())
                .accuracy(report.getAccuracy())
                .intonation(report.getIntonation())
                .detailedAnalysis(detailedAnalysis)
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
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
