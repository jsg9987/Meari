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
    @Query("SELECT r FROM KopicTotalReport r " +
           "JOIN FETCH r.member " +
           "JOIN FETCH r.theme " +
           "WHERE r.member.memberId = :memberId " +
           "AND r.status = 'COMPLETED' " +
           "ORDER BY r.createdAt DESC")
    List<KopicTotalReport> findFirstPageByMemberId(
        @Param("memberId") Long memberId,
        Pageable pageable
    );

    // 커서 기반 페이징: 다음 페이지 조회 (cursor 있음)
    @Query("SELECT r FROM KopicTotalReport r " +
           "JOIN FETCH r.member " +
           "JOIN FETCH r.theme " +
           "WHERE r.member.memberId = :memberId " +
           "AND r.status = 'COMPLETED' " +
           "AND r.createdAt < :cursor " +
           "ORDER BY r.createdAt DESC")
    List<KopicTotalReport> findNextPageByMemberId(
        @Param("memberId") Long memberId,
        @Param("cursor") LocalDateTime cursor,
        Pageable pageable
    );

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
