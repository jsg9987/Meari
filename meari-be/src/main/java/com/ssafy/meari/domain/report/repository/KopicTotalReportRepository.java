package com.ssafy.meari.domain.report.repository;

import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KopicTotalReportRepository extends JpaRepository<KopicTotalReport, Long> {

    @Query("SELECT r FROM KopicTotalReport r " +
           "JOIN FETCH r.member " +
           "JOIN FETCH r.theme " +
           "WHERE r.member.memberId = :memberId " +
           "AND r.status = :status " +
           "ORDER BY r.createdAt DESC")
    List<KopicTotalReport> findRecentCompletedReports(
        @Param("memberId") Long memberId,
        @Param("status") ReportStatus status,
        Pageable pageable
    );

    @Query("SELECT r FROM KopicTotalReport r " +
           "WHERE r.member.memberId = :memberId " +
           "AND r.status = 'COMPLETED' " +
           "ORDER BY r.totalScore DESC")
    List<KopicTotalReport> findTopByMemberIdOrderByTotalScoreDesc(
        @Param("memberId") Long memberId,
        Pageable pageable
    );

    @Query("SELECT r FROM KopicTotalReport r " +
           "WHERE r.member.memberId = :memberId " +
           "AND r.status = 'COMPLETED'")
    List<KopicTotalReport> findAllCompletedByMemberId(@Param("memberId") Long memberId);
}
