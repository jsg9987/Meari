package com.ssafy.meari.domain.report.repository;

import com.ssafy.meari.domain.report.entity.KopicReport;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KopicReportRepository extends JpaRepository<KopicReport, Long> {

    List<KopicReport> findByKopicTotalReportOrderByKopicSentence_KopicSentenceIdAsc(KopicTotalReport kopicTotalReport);

    long countByKopicTotalReportAndStatus(KopicTotalReport kopicTotalReport, ReportStatus status);

    long countByKopicTotalReport(KopicTotalReport kopicTotalReport);

    // 코픽 통합 리포트 ID로 개별 리포트 조회 (문장 ID 오름차순 정렬)
    @Query("SELECT kr FROM KopicReport kr " +
           "JOIN FETCH kr.kopicSentence ks " +
           "WHERE kr.kopicTotalReport.kopicTotalReportId = :totalReportId " +
           "ORDER BY ks.kopicSentenceId ASC")
    List<KopicReport> findByKopicTotalReportIdOrderBySentenceId(@Param("totalReportId") Long totalReportId);
}
