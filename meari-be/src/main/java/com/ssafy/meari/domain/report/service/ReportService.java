package com.ssafy.meari.domain.report.service;

import com.ssafy.meari.domain.report.dto.response.KopicReportDetailItemResponse;
import com.ssafy.meari.domain.report.dto.response.KopicReportListResponse;
import com.ssafy.meari.domain.report.dto.response.KopicTotalReportDetailResponse;
import com.ssafy.meari.domain.report.dto.response.ShadowingReportDetailResponse;
import com.ssafy.meari.domain.report.dto.response.ShadowingReportListResponse;
import com.ssafy.meari.domain.report.entity.KopicReport;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.KopicReportRepository;
import com.ssafy.meari.domain.report.repository.KopicTotalReportRepository;
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
public class ReportService {

    private final ShadowingReportRepository shadowingReportRepository;
    private final KopicTotalReportRepository kopicTotalReportRepository;
    private final KopicReportRepository kopicReportRepository;

    /**
     * 쉐도잉 리포트 목록 조회 (커서 기반 페이징)
     */
    public CursorPageResponse<ShadowingReportListResponse> getShadowingReportList(
            Long memberId, java.time.LocalDateTime cursor, int size) {
        log.debug("쉐도잉 리포트 목록 조회: memberId={}, cursor={}, size={}", memberId, cursor, size);

        // size + 1개 조회해서 다음 페이지 존재 여부 확인
        List<ShadowingReport> reports;
        if (cursor == null) {
            // 첫 페이지: cursor 없이 조회
            reports = shadowingReportRepository.findFirstPageByMemberId(
                    memberId, ReportStatus.COMPLETED, PageRequest.of(0, size + 1));
        } else {
            // 다음 페이지: cursor 이전의 데이터 조회
            reports = shadowingReportRepository.findNextPageByMemberId(
                    memberId, cursor, ReportStatus.COMPLETED, PageRequest.of(0, size + 1));
        }

        boolean hasNext = reports.size() > size;
        if (hasNext) {
            reports = reports.subList(0, size);
        }

        // DTO 변환
        List<ShadowingReportListResponse> contents = reports.stream()
                .map(ShadowingReportListResponse::from)
                .collect(Collectors.toList());

        Long nextCursor = hasNext && !reports.isEmpty()
                ? reports.get(reports.size() - 1).getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;

        return CursorPageResponse.of(contents, nextCursor, hasNext);
    }

    /**
     * 코픽 리포트 목록 조회 (커서 기반 페이징)
     */
    public CursorPageResponse<KopicReportListResponse> getKopicReportList(
            Long memberId, java.time.LocalDateTime cursor, int size) {
        log.debug("코픽 리포트 목록 조회: memberId={}, cursor={}, size={}", memberId, cursor, size);

        // size + 1개 조회해서 다음 페이지 존재 여부 확인
        List<KopicTotalReport> reports;
        if (cursor == null) {
            // 첫 페이지: cursor 없이 조회
            reports = kopicTotalReportRepository.findFirstPageByMemberId(
                    memberId, ReportStatus.COMPLETED, PageRequest.of(0, size + 1));
        } else {
            // 다음 페이지: cursor 이전의 데이터 조회
            reports = kopicTotalReportRepository.findNextPageByMemberId(
                    memberId, cursor, ReportStatus.COMPLETED, PageRequest.of(0, size + 1));
        }

        boolean hasNext = reports.size() > size;
        if (hasNext) {
            reports = reports.subList(0, size);
        }

        // DTO 변환
        List<KopicReportListResponse> contents = reports.stream()
                .map(KopicReportListResponse::from)
                .collect(Collectors.toList());

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

        return ShadowingReportDetailResponse.from(report);
    }

    /**
     * 코픽 통합 리포트 상세 조회 (상태별로 다른 응답)
     * - PROCESSING: 진행 상황 반환 (completed_count/total_count)
     * - COMPLETED: 통합 점수 + 상세 분석 배열 반환, 자동 읽음 처리
     */
    @Transactional
    public KopicTotalReportDetailResponse getKopicTotalReportDetail(Long totalReportId, Long memberId) {
        log.debug("코픽 통합 리포트 상세 조회: totalReportId={}, memberId={}", totalReportId, memberId);

        KopicTotalReport report = kopicTotalReportRepository.findById(totalReportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_KOPIC_TOTAL_REPORT));

        // 본인의 리포트인지 확인
        if (!report.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 상태에 따라 다른 응답 반환
        if (report.getStatus() == ReportStatus.PROCESSING) {
            // PROCESSING 상태: 진행률 정보만 반환
            int completedCount = kopicTotalReportRepository.countCompletedReports(totalReportId);
            int totalCount = report.getSentenceCount();
            return KopicTotalReportDetailResponse.ofProcessing(report, completedCount, totalCount);
        } else {
            // COMPLETED 상태: 통합 점수 + 상세 분석 배열 반환, 읽음 처리
            List<KopicReport> kopicReports = kopicReportRepository.findByKopicTotalReportIdOrderBySentenceId(totalReportId);
            List<KopicReportDetailItemResponse> reportDataItems = kopicReports.stream()
                    .map(KopicReportDetailItemResponse::from)
                    .collect(Collectors.toList());

            // 읽음 처리 (Dirty Checking)
            report.markAsRead();

            return KopicTotalReportDetailResponse.ofCompleted(report, reportDataItems);
        }
    }

}
