package com.ssafy.meari.domain.dashboard.service;

import com.ssafy.meari.domain.dashboard.dto.response.UserActivityResponse;
import com.ssafy.meari.domain.dashboard.entity.DailyRecord;
import com.ssafy.meari.domain.dashboard.mapper.DashboardMapper;
import com.ssafy.meari.domain.dashboard.repository.DailyRecordRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.KopicTotalReportRepository;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dashboard 활동 내역 조회 Service 단위 테스트")
class DashboardServiceActivityTest {

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    @Mock
    private ShadowingReportRepository shadowingReportRepository;

    @Mock
    private KopicTotalReportRepository kopicTotalReportRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private DashboardMapper dashboardMapper;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
            .email("test@example.com")
            .password("password123")
            .nickname("테스터")
            .nativeLanguage(NativeLanguage.KR)
            .build();
    }

    @Test
    @DisplayName("성공 - 모든 타입의 활동 내역 조회")
    void getUserActivities_AllTypes_Success() {
        // Given
        Long memberId = 1L;
        List<DailyRecord> dailyRecords = Arrays.asList(
            createDailyRecord(),
            createDailyRecord()
        );
        List<ShadowingReport> shadowingReports = Arrays.asList(
            createShadowingReport()
        );
        List<KopicTotalReport> kopicReports = Arrays.asList(
            createKopicReport()
        );
        List<UserActivityResponse> expectedActivities = Arrays.asList(
            createActivityResponse("DAILY"),
            createActivityResponse("SHADOWING"),
            createActivityResponse("KOPIC")
        );

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(dailyRecordRepository.findRecentCompletedRecords(eq(memberId), any(PageRequest.class)))
            .willReturn(dailyRecords);
        given(shadowingReportRepository.findRecentCompletedReports(
            eq(memberId), eq(ReportStatus.COMPLETED), any(PageRequest.class)))
            .willReturn(shadowingReports);
        given(kopicTotalReportRepository.findRecentCompletedReports(
            eq(memberId), eq(ReportStatus.COMPLETED), any(PageRequest.class)))
            .willReturn(kopicReports);
        given(dashboardMapper.mergeAndSortActivities(dailyRecords, shadowingReports, kopicReports, 5))
            .willReturn(expectedActivities);

        // When
        List<UserActivityResponse> result = dashboardService.getUserActivities(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(expectedActivities.size());
        verify(memberRepository, times(1)).findById(memberId);
        verify(dailyRecordRepository, times(1)).findRecentCompletedRecords(any(), any());
        verify(shadowingReportRepository, times(1)).findRecentCompletedReports(any(), any(), any());
        verify(kopicTotalReportRepository, times(1)).findRecentCompletedReports(any(), any(), any());
        verify(dashboardMapper, times(1)).mergeAndSortActivities(any(), any(), any(), eq(5));
    }

    @Test
    @DisplayName("성공 - DAILY만 있는 경우")
    void getUserActivities_DailyOnly_Success() {
        // Given
        Long memberId = 1L;
        List<DailyRecord> dailyRecords = Arrays.asList(
            createDailyRecord(),
            createDailyRecord(),
            createDailyRecord()
        );
        List<UserActivityResponse> expectedActivities = Arrays.asList(
            createActivityResponse("DAILY"),
            createActivityResponse("DAILY"),
            createActivityResponse("DAILY")
        );

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(dailyRecordRepository.findRecentCompletedRecords(eq(memberId), any(PageRequest.class)))
            .willReturn(dailyRecords);
        given(shadowingReportRepository.findRecentCompletedReports(
            eq(memberId), eq(ReportStatus.COMPLETED), any(PageRequest.class)))
            .willReturn(Collections.emptyList());
        given(kopicTotalReportRepository.findRecentCompletedReports(
            eq(memberId), eq(ReportStatus.COMPLETED), any(PageRequest.class)))
            .willReturn(Collections.emptyList());
        given(dashboardMapper.mergeAndSortActivities(dailyRecords, Collections.emptyList(), Collections.emptyList(), 5))
            .willReturn(expectedActivities);

        // When
        List<UserActivityResponse> result = dashboardService.getUserActivities(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(3);
        assertThat(result).allMatch(activity -> "DAILY".equals(activity.getActivityType()));
    }

    @Test
    @DisplayName("성공 - 활동 내역 없음")
    void getUserActivities_NoActivities_Success() {
        // Given
        Long memberId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(dailyRecordRepository.findRecentCompletedRecords(eq(memberId), any(PageRequest.class)))
            .willReturn(Collections.emptyList());
        given(shadowingReportRepository.findRecentCompletedReports(
            eq(memberId), eq(ReportStatus.COMPLETED), any(PageRequest.class)))
            .willReturn(Collections.emptyList());
        given(kopicTotalReportRepository.findRecentCompletedReports(
            eq(memberId), eq(ReportStatus.COMPLETED), any(PageRequest.class)))
            .willReturn(Collections.emptyList());
        given(dashboardMapper.mergeAndSortActivities(
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 5))
            .willReturn(Collections.emptyList());

        // When
        List<UserActivityResponse> result = dashboardService.getUserActivities(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 회원")
    void getUserActivities_MemberNotFound_ThrowException() {
        // Given
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> dashboardService.getUserActivities(memberId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_MEMBER);

        verify(memberRepository, times(1)).findById(memberId);
        verify(dailyRecordRepository, times(0)).findRecentCompletedRecords(any(), any());
    }

    @Test
    @DisplayName("성공 - 각 타입별 5개씩 조회 확인")
    void getUserActivities_CheckPageSize_Success() {
        // Given
        Long memberId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(dailyRecordRepository.findRecentCompletedRecords(eq(memberId), any(PageRequest.class)))
            .willReturn(Collections.emptyList());
        given(shadowingReportRepository.findRecentCompletedReports(
            eq(memberId), eq(ReportStatus.COMPLETED), any(PageRequest.class)))
            .willReturn(Collections.emptyList());
        given(kopicTotalReportRepository.findRecentCompletedReports(
            eq(memberId), eq(ReportStatus.COMPLETED), any(PageRequest.class)))
            .willReturn(Collections.emptyList());
        given(dashboardMapper.mergeAndSortActivities(any(), any(), any(), eq(5)))
            .willReturn(Collections.emptyList());

        // When
        dashboardService.getUserActivities(memberId);

        // Then
        verify(dailyRecordRepository).findRecentCompletedRecords(
            eq(memberId),
            eq(PageRequest.of(0, 5))
        );
        verify(shadowingReportRepository).findRecentCompletedReports(
            eq(memberId),
            eq(ReportStatus.COMPLETED),
            eq(PageRequest.of(0, 5))
        );
        verify(kopicTotalReportRepository).findRecentCompletedReports(
            eq(memberId),
            eq(ReportStatus.COMPLETED),
            eq(PageRequest.of(0, 5))
        );
    }

    // 헬퍼 메서드들
    private DailyRecord createDailyRecord() {
        return DailyRecord.builder()
            .member(testMember)
            .recordDate(LocalDate.now())
            .build();
    }

    private ShadowingReport createShadowingReport() {
        // Mock 객체이므로 실제 생성은 필요 없음
        return null;
    }

    private KopicTotalReport createKopicReport() {
        // Mock 객체이므로 실제 생성은 필요 없음
        return null;
    }

    private UserActivityResponse createActivityResponse(String type) {
        return UserActivityResponse.builder()
            .activityType(type)
            .title("Test Activity")
            .status("COMPLETED")
            .build();
    }
}
