package com.ssafy.meari.domain.report.service;

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
                .detailedAnalysis(report.getDetailedAnalysis())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
