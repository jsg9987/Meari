package com.ssafy.meari.domain.dashboard.service;

import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;
import com.ssafy.meari.domain.dashboard.dto.response.KopicDashboardResponse;
import com.ssafy.meari.domain.dashboard.dto.response.UserActivityResponse;
import com.ssafy.meari.domain.dashboard.entity.DailyRecord;
import com.ssafy.meari.domain.dashboard.mapper.DashboardMapper;
import com.ssafy.meari.domain.dashboard.repository.DailyRecordRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.report.entity.KopicReport;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.KopicReportRepository;
import com.ssafy.meari.domain.report.repository.KopicTotalReportRepository;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final DailyRecordRepository dailyRecordRepository;
    private final ShadowingReportRepository shadowingReportRepository;
    private final KopicTotalReportRepository kopicTotalReportRepository;
    private final KopicReportRepository kopicReportRepository;
    private final MemberRepository memberRepository;
    private final DashboardMapper dashboardMapper;

    @Override
    public DailyRecordsResponse getDailyRecords(Long memberId, String period, LocalDate referenceDate) {
        log.debug("일일학습 기록 조회 시작 - memberId: {}, period: {}, referenceDate: {}",
            memberId, period, referenceDate);

        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        // 2. 기간 계산
        LocalDate startDate;
        LocalDate endDate;

        if ("weekly".equalsIgnoreCase(period)) {
            // 월요일~일요일
            startDate = referenceDate.with(DayOfWeek.MONDAY);
            endDate = referenceDate.with(DayOfWeek.SUNDAY);
        } else if ("monthly".equalsIgnoreCase(period)) {
            // 1일~마지막날
            startDate = referenceDate.withDayOfMonth(1);
            endDate = referenceDate.with(TemporalAdjusters.lastDayOfMonth());
        } else if ("yearly".equalsIgnoreCase(period)) {
            // 1월 1일~12월 31일
            startDate = referenceDate.withDayOfYear(1);
            endDate = referenceDate.withDayOfYear(referenceDate.lengthOfYear());
        } else {
            throw new BusinessException(ErrorCode.INVALID_PERIOD);
        }

        log.debug("조회 기간 계산 완료 - startDate: {}, endDate: {}", startDate, endDate);

        // 3. DB 조회 (학습 완료한 날짜만)
        List<DailyRecord> records = dailyRecordRepository
            .findCompletedRecordsByMemberAndDateRange(member, startDate, endDate);

        log.debug("조회된 학습 기록 수: {}", records.size());

        // 4. DTO 변환
        return dashboardMapper.toResponse(startDate, endDate, records);
    }

    @Override
    @Transactional
    public void recordLearningCompletion(Long memberId, LocalDate completionDate) {
        log.debug("학습 완료 기록 시작 - memberId: {}, date: {}", memberId, completionDate);

        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        // 2. 해당 날짜 기록 조회 또는 생성
        DailyRecord record = dailyRecordRepository
            .findByMemberAndRecordDate(member, completionDate)
            .orElseGet(() -> DailyRecord.builder()
                .member(member)
                .recordDate(completionDate)
                .build());

        // 3. 완료 개수 증가 (Dirty Checking)
        record.incrementCompletedCount();

        // 4. 저장 (새로 생성된 경우만)
        if (record.getDailyRecordId() == null) {
            dailyRecordRepository.save(record);
        }

        log.debug("학습 완료 기록 완료 - completedCount: {}", record.getCompletedCount());
    }

    @Override
    public List<UserActivityResponse> getUserActivities(Long memberId) {
        log.debug("사용자 활동 내역 조회 시작 - memberId: {}", memberId);

        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        // 2. 각 타입별로 최신 5개씩 조회
        List<DailyRecord> dailyRecords = dailyRecordRepository
            .findRecentCompletedRecords(memberId, PageRequest.of(0, 5));

        List<ShadowingReport> shadowingReports = shadowingReportRepository
            .findRecentCompletedReports(memberId, ReportStatus.COMPLETED,
                PageRequest.of(0, 5));

        List<KopicTotalReport> kopicReports = kopicTotalReportRepository
            .findRecentCompletedReports(memberId, ReportStatus.COMPLETED,
                PageRequest.of(0, 5));

        // 3. 통합 및 정렬 후 최신 5개만 반환
        List<UserActivityResponse> activities = dashboardMapper
            .mergeAndSortActivities(dailyRecords, shadowingReports, kopicReports, 5);

        log.debug("활동 내역 조회 완료 - 총 {}건", activities.size());

        return activities;
    }

    @Override
    public KopicDashboardResponse getKopicDashboard(Long memberId) {
        log.debug("코픽 대시보드 조회 시작 - memberId: {}", memberId);

        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        // 2. 완료된 모든 코픽 리포트 조회
        List<KopicTotalReport> allReports = kopicTotalReportRepository
                .findAllCompletedByMemberId(memberId);

        if (allReports.isEmpty()) {
            log.debug("완료된 코픽 리포트가 없음 - memberId: {}", memberId);
            return KopicDashboardResponse.builder()
                    .best(null)
                    .average(null)
                    .build();
        }

        // 3. 최고 점수 리포트 찾기 (null 체크 추가)
        KopicTotalReport bestReport = allReports.stream()
                .filter(report -> report.getTotalScore() != null)
                .max(Comparator.comparing(KopicTotalReport::getTotalScore))
                .orElse(null);

        if (bestReport == null) {
            log.debug("유효한 점수를 가진 리포트가 없음 - memberId: {}", memberId);
            return KopicDashboardResponse.builder()
                    .best(null)
                    .average(null)
                    .build();
        }

        // 4. 최고 점수 리포트의 각 문장별 점수 조회
        List<KopicReport> bestReportDetails = kopicReportRepository
                .findByKopicTotalReportOrderByKopicSentence_KopicSentenceIdAsc(bestReport);

        List<KopicDashboardResponse.SentenceScore> bestSentenceScores = bestReportDetails.stream()
                .filter(report -> report.getTotalScore() != null)
                .map(report -> KopicDashboardResponse.SentenceScore.builder()
                        .kopicSentenceId(report.getKopicSentence().getKopicSentenceId())
                        .score(report.getTotalScore())
                        .build())
                .collect(Collectors.toList());

        // 5. 전체 평균 계산 (null 필터링)
        int totalAvgScore = (int) allReports.stream()
                .filter(report -> report.getTotalScore() != null)
                .mapToInt(KopicTotalReport::getTotalScore)
                .average()
                .orElse(0);

        // 6. 각 문장별 평균 점수 계산
        Map<Long, List<Integer>> sentenceScoresMap = new HashMap<>();

        for (KopicTotalReport report : allReports) {
            List<KopicReport> reportDetails = kopicReportRepository
                    .findByKopicTotalReportOrderByKopicSentence_KopicSentenceIdAsc(report);

            for (KopicReport detail : reportDetails) {
                if (detail.getTotalScore() != null) {
                    Long sentenceId = detail.getKopicSentence().getKopicSentenceId();
                    sentenceScoresMap.computeIfAbsent(sentenceId, k -> new ArrayList<>())
                            .add(detail.getTotalScore());
                }
            }
        }

        List<KopicDashboardResponse.SentenceAvgScore> avgSentenceScores = sentenceScoresMap.entrySet().stream()
                .map(entry -> {
                    Long sentenceId = entry.getKey();
                    List<Integer> scores = entry.getValue();
                    int avgScore = (int) scores.stream()
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0);

                    return KopicDashboardResponse.SentenceAvgScore.builder()
                            .kopicSentenceId(sentenceId)
                            .avgScore(avgScore)
                            .build();
                })
                .sorted(Comparator.comparing(KopicDashboardResponse.SentenceAvgScore::getKopicSentenceId))
                .collect(Collectors.toList());

        // 7. 응답 생성
        KopicDashboardResponse.BestExamData bestExamData = KopicDashboardResponse.BestExamData.builder()
                .examDate(bestReport.getCreatedAt().toLocalDate())
                .totalAvgScore(bestReport.getTotalScore() != null ? bestReport.getTotalScore() : 0)
                .sentences(bestSentenceScores)
                .build();

        KopicDashboardResponse.AverageScoreData averageScoreData = KopicDashboardResponse.AverageScoreData.builder()
                .totalAvgScore(totalAvgScore)
                .sentences(avgSentenceScores)
                .build();

        log.debug("코픽 대시보드 조회 완료 - 최고 점수: {}, 평균 점수: {}",
                bestReport.getTotalScore(), totalAvgScore);

        return KopicDashboardResponse.builder()
                .best(bestExamData)
                .average(averageScoreData)
                .build();
    }
}
