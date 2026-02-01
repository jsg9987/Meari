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

    }
