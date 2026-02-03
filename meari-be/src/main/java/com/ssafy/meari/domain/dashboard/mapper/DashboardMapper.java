package com.ssafy.meari.domain.dashboard.mapper;

import com.ssafy.meari.domain.dashboard.dto.response.ActivityResponse;
import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;
import com.ssafy.meari.domain.dashboard.entity.DailyRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
}
