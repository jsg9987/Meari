package com.ssafy.meari.domain.report.repository;

import com.ssafy.meari.domain.report.entity.ShadowingReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShadowingReportRepository extends JpaRepository<ShadowingReport, Long> {

    /**
     * roomId, round, memberId로 리포트 조회
     * (room_id, round, member_id)는 UNIQUE 제약조건이므로 최대 1개 반환
     */
    Optional<ShadowingReport> findByRoom_RoomIdAndRoundAndMember_MemberId(Long roomId, Integer round, Long memberId);
}
