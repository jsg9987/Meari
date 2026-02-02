package com.ssafy.meari.domain.dashboard.repository;

import com.ssafy.meari.domain.dashboard.entity.DailyRecord;
import com.ssafy.meari.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    /**
     * 사용자의 특정 날짜 기록 조회
     * @param member 회원 엔티티
     * @param recordDate 조회할 날짜
     * @return 일일 학습 기록 (Optional)
     */
    Optional<DailyRecord> findByMemberAndRecordDate(Member member, LocalDate recordDate);

    /**
     * 사용자의 기간별 기록 조회 (주/월 단위)
     * @param member 회원 엔티티
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 기간 내 모든 학습 기록 리스트
     */
    @Query("SELECT d FROM DailyRecord d " +
           "WHERE d.member = :member " +
           "AND d.recordDate BETWEEN :startDate AND :endDate " +
           "ORDER BY d.recordDate ASC")
    List<DailyRecord> findByMemberAndDateRange(
        @Param("member") Member member,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 학습 완료한 날짜만 조회 (성능 최적화)
     * level > 0인 레코드만 반환 (단어 학습 또는 문장 퀴즈 중 하나라도 완료한 날짜)
     * @param member 회원 엔티티
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 기간 내 학습 완료한 날짜의 기록 리스트
     */
    @Query("SELECT d FROM DailyRecord d " +
           "WHERE d.member = :member " +
           "AND d.recordDate BETWEEN :startDate AND :endDate " +
           "AND (d.isWordStudyFinished = true OR d.isSentenceQuizFinished = true) " +
           "ORDER BY d.recordDate ASC")
    List<DailyRecord> findCompletedRecordsByMemberAndDateRange(
        @Param("member") Member member,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 사용자의 전체 학습 일수 조회 (통계용)
     * @param member 회원 엔티티
     * @return 학습을 진행한 총 일수
     */
    @Query("SELECT COUNT(d) FROM DailyRecord d " +
           "WHERE d.member = :member " +
           "AND (d.isWordStudyFinished = true OR d.isSentenceQuizFinished = true)")
    Long countCompletedDaysByMember(@Param("member") Member member);
}
