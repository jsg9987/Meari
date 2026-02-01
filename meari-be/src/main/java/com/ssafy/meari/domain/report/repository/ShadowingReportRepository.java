package com.ssafy.meari.domain.report.repository;

import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShadowingReportRepository extends JpaRepository<ShadowingReport, Long> {

    // 커서 기반 페이징: 특정 회원의 쉐도잉 리포트 목록 조회 (created_at 기준 내림차순)
    @Query("SELECT sr FROM ShadowingReport sr " +
           "JOIN FETCH sr.member m " +
           "JOIN FETCH sr.room r " +
           "JOIN FETCH sr.content c " +
           "JOIN FETCH sr.role ro " +
           "WHERE m.memberId = :memberId " +
           "AND sr.status = :status " +
           "AND (:cursor IS NULL OR sr.createdAt < :cursor) " +
           "ORDER BY sr.createdAt DESC")
    List<ShadowingReport> findByMemberIdWithCursor(
            @Param("memberId") Long memberId,
            @Param("cursor") LocalDateTime cursor,
            @Param("status") ReportStatus status,
            Pageable pageable
    );
}
