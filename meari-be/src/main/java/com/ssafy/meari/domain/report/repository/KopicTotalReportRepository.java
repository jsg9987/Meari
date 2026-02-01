package com.ssafy.meari.domain.report.repository;

import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KopicTotalReportRepository extends JpaRepository<KopicTotalReport, Long> {

    // 커서 기반 페이징: 첫 페이지 조회 (cursor 없음)
    @Query(value = "SELECT ktr FROM KopicTotalReport ktr " +
           "JOIN FETCH ktr.member m " +
           "JOIN FETCH ktr.theme t " +
           "WHERE m.memberId = :memberId " +
           "AND ktr.status = :status " +
           "ORDER BY ktr.createdAt DESC")
    List<KopicTotalReport> findFirstPageByMemberId(
            @Param("memberId") Long memberId,
            @Param("status") ReportStatus status,
            Pageable pageable
    );

    // 커서 기반 페이징: 다음 페이지 조회 (cursor 있음)
    @Query(value = "SELECT ktr FROM KopicTotalReport ktr " +
           "JOIN FETCH ktr.member m " +
           "JOIN FETCH ktr.theme t " +
           "WHERE m.memberId = :memberId " +
           "AND ktr.status = :status " +
           "AND ktr.createdAt < :cursor " +
           "ORDER BY ktr.createdAt DESC")
    List<KopicTotalReport> findNextPageByMemberId(
            @Param("memberId") Long memberId,
            @Param("cursor") LocalDateTime cursor,
            @Param("status") ReportStatus status,
            Pageable pageable
    );

    // PROCESSING 상태 리포트에서 완료된 개별 리포트 개수 조회
    @Query("SELECT COUNT(kr) FROM KopicReport kr " +
           "WHERE kr.kopicTotalReport.kopicTotalReportId = :totalReportId " +
           "AND kr.status = 'COMPLETED'")
    int countCompletedReports(@Param("totalReportId") Long totalReportId);
}
