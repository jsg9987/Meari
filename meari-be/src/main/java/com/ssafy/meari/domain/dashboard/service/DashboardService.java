package com.ssafy.meari.domain.dashboard.service;

import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;

import java.time.LocalDate;

public interface DashboardService {

    /**
     * 일일학습 기록 조회 (주/월 단위)
     * @param memberId 회원 ID
     * @param period "weekly" 또는 "monthly"
     * @param referenceDate 기준 날짜 (optional, 기본값: 오늘)
     * @return 기간별 학습 기록
     */
    DailyRecordsResponse getDailyRecords(Long memberId, String period, LocalDate referenceDate);

    /**
     * 학습 완료 기록 (다른 서비스에서 호출)
     * @param memberId 회원 ID
     * @param learningType "WORD_STUDY" 또는 "SENTENCE_QUIZ"
     * @param completionDate 완료 날짜
     */
    void recordLearningCompletion(Long memberId, String learningType, LocalDate completionDate);
}
