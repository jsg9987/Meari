package com.ssafy.meari.domain.dashboard.mapper;

import com.ssafy.meari.domain.dashboard.dto.response.ActivityResponse;
import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;
import com.ssafy.meari.domain.dashboard.dto.response.UserActivityResponse;
import com.ssafy.meari.domain.dashboard.entity.DailyRecord;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DashboardMapper {

    /**
     * DailyRecord 리스트를 DailyRecordsResponse로 변환
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @param records 일일 학습 기록 리스트
     * @return 응답 DTO
     */
    public DailyRecordsResponse toResponse(LocalDate startDate, LocalDate endDate,
                                           List<DailyRecord> records) {
        List<ActivityResponse> activities = records.stream()
            .filter(DailyRecord::hasAnyCompletion) // completedCount > 0인 것만
            .map(this::toActivityResponse)
            .collect(Collectors.toList());

        return DailyRecordsResponse.builder()
            .startDate(startDate.toString())
            .endDate(endDate.toString())
            .activities(activities)
            .build();
    }

    /**
     * DailyRecord를 ActivityResponse로 변환
     * @param record 일일 학습 기록
     * @return 활동 응답 DTO
     */
    private ActivityResponse toActivityResponse(DailyRecord record) {
        return ActivityResponse.builder()
            .date(record.getRecordDate().toString())
            .completed_count(record.getLevel())
            .build();
    }

    /**
     * DailyRecord를 사용자 활동 내역으로 변환
     * @param record 일일 학습 기록
     * @return 사용자 활동 응답 DTO
     */
    public UserActivityResponse toDailyActivity(DailyRecord record) {
        return UserActivityResponse.builder()
            .activityType("DAILY")
            .title("일일학습 완료했습니다!")
            .status("COMPLETED")
            .createdAt(record.getCreatedAt())
            .build();
    }

    /**
     * ShadowingReport를 사용자 활동 내역으로 변환
     * @param report 쉐도잉 리포트
     * @return 사용자 활동 응답 DTO
     */
    public UserActivityResponse toShadowingActivity(ShadowingReport report) {
        String themeName = report.getRoom().getTheme().getName();
        String contentTitle = report.getContent().getTitle();

        return UserActivityResponse.builder()
            .activityType("SHADOWING")
            .theme(themeName)
            .content(contentTitle)
            .title(String.format("쉐도잉(%s) - %s를 완료했습니다!", themeName, contentTitle))
            .status(report.getStatus().name())
            .createdAt(report.getCreatedAt())
            .build();
    }

    /**
     * KopicTotalReport를 사용자 활동 내역으로 변환
     * @param report KOPIC 총합 리포트
     * @return 사용자 활동 응답 DTO
     */
    public UserActivityResponse toKopicActivity(KopicTotalReport report) {
        String themeName = report.getTheme().getName();

        return UserActivityResponse.builder()
            .activityType("KOPIC")
            .theme(themeName)
            .title(String.format("KOPIC(%s) 채점이 완료되었습니다!", themeName))
            .status(report.getStatus().name())
            .createdAt(report.getCreatedAt())
            .build();
    }

    /**
     * 여러 활동 내역을 통합하고 정렬 후 제한된 개수만 반환
     * @param dailyRecords 일일 학습 기록 리스트
     * @param shadowingReports 쉐도잉 리포트 리스트
     * @param kopicReports KOPIC 리포트 리스트
     * @param limit 반환할 최대 개수
     * @return 통합 정렬된 활동 내역 리스트
     */
    public List<UserActivityResponse> mergeAndSortActivities(
        List<DailyRecord> dailyRecords,
        List<ShadowingReport> shadowingReports,
        List<KopicTotalReport> kopicReports,
        int limit
    ) {
        List<UserActivityResponse> activities = new ArrayList<>();

        // 각 타입별로 변환하여 리스트에 추가
        dailyRecords.forEach(dr -> activities.add(toDailyActivity(dr)));
        shadowingReports.forEach(sr -> activities.add(toShadowingActivity(sr)));
        kopicReports.forEach(kr -> activities.add(toKopicActivity(kr)));

        // createdAt 내림차순 정렬
        activities.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // limit 개수만큼만 반환
        return activities.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
}
