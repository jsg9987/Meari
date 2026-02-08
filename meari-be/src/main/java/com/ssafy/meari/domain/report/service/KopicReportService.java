package com.ssafy.meari.domain.report.service;

import com.ssafy.meari.domain.report.dto.response.KopicTotalReportListItemResponse;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.repository.KopicTotalReportRepository;
import com.ssafy.meari.global.common.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KopicReportService {

    private final KopicTotalReportRepository kopicTotalReportRepository;

    /**
     * 코픽 통합 리포트 목록 조회 (커서 기반 페이징)
     */
    public CursorPageResponse<KopicTotalReportListItemResponse> getKopicTotalReportList(
            Long memberId, LocalDateTime cursor, int size) {
        log.debug("코픽 통합 리포트 목록 조회: memberId={}, cursor={}, size={}", memberId, cursor, size);

        // size + 1개 조회해서 다음 페이지 존재 여부 확인
        List<KopicTotalReport> reports;
        if (cursor == null) {
            reports = kopicTotalReportRepository.findFirstPageByMemberId(
                    memberId, PageRequest.of(0, size + 1));
        } else {
            reports = kopicTotalReportRepository.findNextPageByMemberId(
                    memberId, cursor, PageRequest.of(0, size + 1));
        }

        boolean hasNext = reports.size() > size;
        if (hasNext) {
            reports = reports.subList(0, size);
        }

        List<KopicTotalReportListItemResponse> contents = reports.stream()
                .map(KopicTotalReportListItemResponse::from)
                .collect(Collectors.toList());

        Long nextCursor = hasNext && !reports.isEmpty()
                ? reports.get(reports.size() - 1).getCreatedAt()
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;

        return CursorPageResponse.of(contents, nextCursor, hasNext);
    }
}
