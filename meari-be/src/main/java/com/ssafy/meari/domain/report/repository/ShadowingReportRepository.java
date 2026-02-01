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

    // 커서 기반 페이징: 첫 페이지 조회 (cursor 없음)
    @Query(value = "SELECT sr FROM ShadowingReport sr " +
           "JOIN FETCH sr.member m " +
           "JOIN FETCH sr.room r " +
           "JOIN FETCH sr.content c " +
           "JOIN FETCH sr.role ro " +
           "WHERE m.memberId = :memberId " +
           "AND sr.status = :status " +
           "ORDER BY sr.createdAt DESC")
    List<ShadowingReport> findFirstPageByMemberId(
            @Param("memberId") Long memberId,
            @Param("status") ReportStatus status,
            Pageable pageable
    );

    // 커서 기반 페이징: 다음 페이지 조회 (cursor 있음)
    @Query(value = "SELECT sr FROM ShadowingReport sr " +
           "JOIN FETCH sr.member m " +
           "JOIN FETCH sr.room r " +
           "JOIN FETCH sr.content c " +
           "JOIN FETCH sr.role ro " +
           "WHERE m.memberId = :memberId " +
           "AND sr.status = :status " +
           "AND sr.createdAt < :cursor " +
           "ORDER BY sr.createdAt DESC")
    List<ShadowingReport> findNextPageByMemberId(
            @Param("memberId") Long memberId,
            @Param("cursor") LocalDateTime cursor,
            @Param("status") ReportStatus status,
            Pageable pageable
    );
}
