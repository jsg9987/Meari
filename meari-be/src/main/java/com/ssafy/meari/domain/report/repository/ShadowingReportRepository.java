package com.ssafy.meari.domain.report.repository;

import com.ssafy.meari.domain.report.entity.ShadowingReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShadowingReportRepository extends JpaRepository<ShadowingReport, Long> {
}
