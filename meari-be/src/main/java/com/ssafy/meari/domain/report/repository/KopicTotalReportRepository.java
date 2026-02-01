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

    // 커서 기반 페이징: 특정 회원의 코픽 종합 리포트 목록 조회 (created_at 기준 내림차순)
    @Query("SELECT ktr FROM KopicTotalReport ktr " +
           "JOIN FETCH ktr.member m " +
           "JOIN FETCH ktr.theme t " +
           "WHERE m.memberId = :memberId " +
           "AND ktr.status = :status " +
           "AND (:cursor IS NULL OR ktr.createdAt < :cursor) " +
           "ORDER BY ktr.createdAt DESC")
    List<KopicTotalReport> findByMemberIdWithCursor(
            @Param("memberId") Long memberId,
            @Param("cursor") LocalDateTime cursor,
            @Param("status") ReportStatus status,
            Pageable pageable
    );
}
