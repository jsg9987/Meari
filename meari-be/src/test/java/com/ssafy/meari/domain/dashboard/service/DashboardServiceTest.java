package com.ssafy.meari.domain.dashboard.service;

import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;
import com.ssafy.meari.domain.dashboard.entity.DailyRecord;
import com.ssafy.meari.domain.dashboard.mapper.DashboardMapper;
import com.ssafy.meari.domain.dashboard.repository.DailyRecordRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService 단위 테스트")
class DashboardServiceTest {

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private DashboardMapper dashboardMapper;

    private Member testMember;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스터")
                .nativeLanguage(NativeLanguage.KR)
                .build();
        testDate = LocalDate.of(2025, 2, 2);
    }

    @Test
    @DisplayName("성공 - 주간 기록 조회")
    void getDailyRecords_Weekly_Success() {
        // Given
        Long memberId = 1L;
        LocalDate monday = testDate.with(DayOfWeek.MONDAY);
        LocalDate sunday = testDate.with(DayOfWeek.SUNDAY);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(dailyRecordRepository.findCompletedRecordsByMemberAndDateRange(
                eq(testMember), eq(monday), eq(sunday))).willReturn(Arrays.asList());
        given(dashboardMapper.toResponse(any(), any(), any()))
                .willReturn(DailyRecordsResponse.builder().build());

        // When
        DailyRecordsResponse response = dashboardService.getDailyRecords(memberId, "weekly", testDate);

        // Then
        assertThat(response).isNotNull();
        verify(memberRepository, times(1)).findById(memberId);
        verify(dailyRecordRepository, times(1))
                .findCompletedRecordsByMemberAndDateRange(testMember, monday, sunday);
    }

    @Test
    @DisplayName("성공 - 학습 완료 기록 (신규)")
    void recordLearningCompletion_NewRecord_Success() {
        // Given
        Long memberId = 1L;
        LocalDate completionDate = testDate;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(dailyRecordRepository.findByMemberAndRecordDate(testMember, completionDate))
                .willReturn(Optional.empty());

        // When
        dashboardService.recordLearningCompletion(memberId, completionDate);

        // Then
        verify(dailyRecordRepository, times(1)).save(any(DailyRecord.class));
    }

    @Test
    @DisplayName("성공 - 학습 완료 기록 (기존 레코드 업데이트)")
    void recordLearningCompletion_ExistingRecord_Success() {
        // Given
        Long memberId = 1L;
        LocalDate completionDate = testDate;

        DailyRecord existingRecord = DailyRecord.builder()
                .member(testMember)
                .recordDate(completionDate)
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(dailyRecordRepository.findByMemberAndRecordDate(testMember, completionDate))
                .willReturn(Optional.of(existingRecord));

        // When
        dashboardService.recordLearningCompletion(memberId, completionDate);

        // Then
        verify(dailyRecordRepository, never()).save(any());  // Dirty Checking
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 회원")
    void getDailyRecords_MemberNotFound_ThrowException() {
        // Given
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> dashboardService.getDailyRecords(memberId, "weekly", testDate))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_MEMBER);
    }

    @Test
    @DisplayName("성공 - 년간 기록 조회")
    void getDailyRecords_Yearly_Success() {
        // Given
        Long memberId = 1L;
        LocalDate jan1 = testDate.withDayOfYear(1);  // 2025-01-01
        LocalDate dec31 = testDate.withDayOfYear(testDate.lengthOfYear());  // 2025-12-31

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(dailyRecordRepository.findCompletedRecordsByMemberAndDateRange(
                eq(testMember), eq(jan1), eq(dec31))).willReturn(Arrays.asList());
        given(dashboardMapper.toResponse(any(), any(), any()))
                .willReturn(DailyRecordsResponse.builder().build());

        // When
        DailyRecordsResponse response = dashboardService.getDailyRecords(memberId, "yearly", testDate);

        // Then
        assertThat(response).isNotNull();
        verify(memberRepository, times(1)).findById(memberId);
        verify(dailyRecordRepository, times(1))
                .findCompletedRecordsByMemberAndDateRange(testMember, jan1, dec31);
    }

    @Test
    @DisplayName("실패 - 유효하지 않은 period")
    void getDailyRecords_InvalidPeriod_ThrowException() {
        // Given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));

        // When & Then
        assertThatThrownBy(() -> dashboardService.getDailyRecords(memberId, "invalid", testDate))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PERIOD);
    }
}
