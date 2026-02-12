package com.ssafy.meari.domain.dashboard.service;

import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;
import com.ssafy.meari.domain.dashboard.dto.response.KopicDashboardResponse;
import com.ssafy.meari.domain.dashboard.dto.response.UserActivityResponse;

import java.time.LocalDate;
import java.util.List;

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

    /**
     * 사용자 최근 활동 내역 조회 (일일학습, 쉐도잉, KOPIC 통합)
     * - 각 타입별로 최신 5개씩 조회 후 통합 정렬
     * - 최종적으로 최신순 5개만 반환
     * @param memberId 회원 ID
     * @return 최근 활동 내역 리스트 (최대 5개)
     */
    List<UserActivityResponse> getUserActivities(Long memberId);

    /**
     * 코픽 대시보드 점수 요약 조회
     * - 최고 점수 시험과 전체 평균 점수 비교
     * - 문장별 점수 및 평균 점수 포함
     * @param memberId 회원 ID
     * @return 코픽 대시보드 응답
     */
    KopicDashboardResponse getKopicDashboard(Long memberId);
}
