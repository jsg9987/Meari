package com.ssafy.meari.domain.dashboard.service;

import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;

import java.time.LocalDate;

public interface DashboardService {

    /**
     * 일일학습 기록 조회 (주/월/년 단위)
     * @param memberId 회원 ID
     * @param period "weekly", "monthly", "yearly"
     * @param referenceDate 기준 날짜 (optional, 기본값: 오늘)
     * @return 기간별 학습 기록
     */
    DailyRecordsResponse getDailyRecords(Long memberId, String period, LocalDate referenceDate);

    /**
     * 학습 완료 기록 (다른 서비스에서 호출)
     * - learningType 파라미터 제거: 어떤 학습인지 구분하지 않고 completedCount만 증가
     * - 프론트에서 하루 학습량을 제한하므로 백엔드는 단순히 카운트만 관리
     * @param memberId 회원 ID
     * @param completionDate 완료 날짜
     */
    void recordLearningCompletion(Long memberId, LocalDate completionDate);
}
