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

    /**
     * 최근 완료된 KOPIC 총합 리포트 조회 (활동 내역용, N+1 방지)
     * @param memberId 회원 ID
     * @param status 리포트 상태
     * @param pageable 페이징 정보
     * @return 최근 완료 리포트 리스트 (theme JOIN FETCH)
     */
    @Query("SELECT ktr FROM KopicTotalReport ktr " +
           "JOIN FETCH ktr.theme " +
           "WHERE ktr.member.memberId = :memberId " +
           "AND ktr.status = :status " +
           "ORDER BY ktr.createdAt DESC")
    List<KopicTotalReport> findRecentCompletedReports(
        @Param("memberId") Long memberId,
        @Param("status") ReportStatus status,
        Pageable pageable
    );
}
