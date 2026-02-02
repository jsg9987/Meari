package com.ssafy.meari.domain.dashboard.service;

import com.ssafy.meari.domain.dashboard.dto.response.ActivityResponse;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
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

        testDate = LocalDate.of(2025, 2, 2); // 일요일
    }

    @Nested
    @DisplayName("일일학습 기록 조회")
    class GetDailyRecords {

        @Test
        @DisplayName("성공 - 주간 기록 조회 (월요일~일요일)")
        void getDailyRecords_Weekly_Success() {
            // Given
            Long memberId = 1L;
            LocalDate monday = testDate.with(DayOfWeek.MONDAY);
            LocalDate sunday = testDate.with(DayOfWeek.SUNDAY);

            List<DailyRecord> mockRecords = Arrays.asList(
                    createMockRecord(testMember, monday.plusDays(2), true, false),
                    createMockRecord(testMember, monday.plusDays(4), true, true)
            );

            DailyRecordsResponse expectedResponse = DailyRecordsResponse.builder()
                    .startDate(monday.toString())
                    .endDate(sunday.toString())
                    .activities(Arrays.asList(
                            ActivityResponse.builder().date(monday.plusDays(2).toString()).level(1).build(),
                            ActivityResponse.builder().date(monday.plusDays(4).toString()).level(2).build()
                    ))
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
            given(dailyRecordRepository.findCompletedRecordsByMemberAndDateRange(
                    eq(testMember), eq(monday), eq(sunday))).willReturn(mockRecords);
            given(dashboardMapper.toResponse(eq(monday), eq(sunday), eq(mockRecords)))
                    .willReturn(expectedResponse);

            // When
            DailyRecordsResponse response = dashboardService.getDailyRecords(memberId, "weekly", testDate);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStartDate()).isEqualTo(monday.toString());
            assertThat(response.getEndDate()).isEqualTo(sunday.toString());
            assertThat(response.getActivities()).hasSize(2);

            verify(memberRepository, times(1)).findById(memberId);
            verify(dailyRecordRepository, times(1))
                    .findCompletedRecordsByMemberAndDateRange(testMember, monday, sunday);
            verify(dashboardMapper, times(1)).toResponse(monday, sunday, mockRecords);
        }

        @Test
        @DisplayName("성공 - 월간 기록 조회 (1일~마지막날)")
        void getDailyRecords_Monthly_Success() {
            // Given
            Long memberId = 1L;
            LocalDate firstDay = testDate.withDayOfMonth(1);
            LocalDate lastDay = testDate.with(TemporalAdjusters.lastDayOfMonth());

            List<DailyRecord> mockRecords = Arrays.asList(
                    createMockRecord(testMember, firstDay, true, false),
                    createMockRecord(testMember, firstDay.plusDays(10), true, true)
            );

            DailyRecordsResponse expectedResponse = DailyRecordsResponse.builder()
                    .startDate(firstDay.toString())
                    .endDate(lastDay.toString())
                    .activities(Arrays.asList(
                            ActivityResponse.builder().date(firstDay.toString()).level(1).build(),
                            ActivityResponse.builder().date(firstDay.plusDays(10).toString()).level(2).build()
                    ))
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
            given(dailyRecordRepository.findCompletedRecordsByMemberAndDateRange(
                    eq(testMember), eq(firstDay), eq(lastDay))).willReturn(mockRecords);
            given(dashboardMapper.toResponse(eq(firstDay), eq(lastDay), eq(mockRecords)))
                    .willReturn(expectedResponse);

            // When
            DailyRecordsResponse response = dashboardService.getDailyRecords(memberId, "monthly", testDate);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStartDate()).isEqualTo(firstDay.toString());
            assertThat(response.getEndDate()).isEqualTo(lastDay.toString());
            assertThat(response.getActivities()).hasSize(2);

            verify(memberRepository, times(1)).findById(memberId);
            verify(dailyRecordRepository, times(1))
                    .findCompletedRecordsByMemberAndDateRange(testMember, firstDay, lastDay);
            verify(dashboardMapper, times(1)).toResponse(firstDay, lastDay, mockRecords);
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

            verify(memberRepository, times(1)).findById(memberId);
            verify(dailyRecordRepository, never()).findCompletedRecordsByMemberAndDateRange(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 period")
        void getDailyRecords_InvalidPeriod_ThrowException() {
            // Given
            Long memberId = 1L;
            String invalidPeriod = "yearly";

            given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));

            // When & Then
            assertThatThrownBy(() -> dashboardService.getDailyRecords(memberId, invalidPeriod, testDate))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PERIOD);

            verify(memberRepository, times(1)).findById(memberId);
            verify(dailyRecordRepository, never()).findCompletedRecordsByMemberAndDateRange(any(), any(), any());
        }

        @Test
        @DisplayName("성공 - period 대소문자 구분 없음")
        void getDailyRecords_CaseInsensitive_Success() {
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
            dashboardService.getDailyRecords(memberId, "WEEKLY", testDate);
            dashboardService.getDailyRecords(memberId, "Weekly", testDate);
            dashboardService.getDailyRecords(memberId, "wEeKlY", testDate);

            // Then
            verify(memberRepository, times(3)).findById(memberId);
            verify(dailyRecordRepository, times(3))
                    .findCompletedRecordsByMemberAndDateRange(testMember, monday, sunday);
        }
    }

    @Nested
    @DisplayName("학습 완료 기록")
    class RecordLearningCompletion {

        @Test
        @DisplayName("성공 - 단어 학습 완료 기록 (신규 생성)")
        void recordLearningCompletion_WordStudy_NewRecord_Success() {
            // Given
            Long memberId = 1L;
            LocalDate completionDate = testDate;

            DailyRecord newRecord = DailyRecord.builder()
                    .member(testMember)
                    .recordDate(completionDate)
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
            given(dailyRecordRepository.findByMemberAndRecordDate(testMember, completionDate))
                    .willReturn(Optional.empty());
            given(dailyRecordRepository.save(any(DailyRecord.class))).willReturn(newRecord);

            // When
            dashboardService.recordLearningCompletion(memberId, "WORD_STUDY", completionDate);

            // Then
            verify(memberRepository, times(1)).findById(memberId);
            verify(dailyRecordRepository, times(1))
                    .findByMemberAndRecordDate(testMember, completionDate);
            verify(dailyRecordRepository, times(1)).save(any(DailyRecord.class));
        }

        @Test
        @DisplayName("성공 - 문장 퀴즈 완료 기록 (기존 레코드 업데이트)")
        void recordLearningCompletion_SentenceQuiz_ExistingRecord_Success() {
            // Given
            Long memberId = 1L;
            LocalDate completionDate = testDate;

            DailyRecord existingRecord = createMockRecord(testMember, completionDate, true, false);

            given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
            given(dailyRecordRepository.findByMemberAndRecordDate(testMember, completionDate))
                    .willReturn(Optional.of(existingRecord));

            // When
            dashboardService.recordLearningCompletion(memberId, "SENTENCE_QUIZ", completionDate);

            // Then
            verify(memberRepository, times(1)).findById(memberId);
            verify(dailyRecordRepository, times(1))
                    .findByMemberAndRecordDate(testMember, completionDate);
            verify(dailyRecordRepository, never()).save(any()); // Dirty Checking이므로 save 호출 안함
        }

        @Test
        @DisplayName("성공 - learningType 대소문자 구분 없음")
        void recordLearningCompletion_CaseInsensitive_Success() {
            // Given
            Long memberId = 1L;
            LocalDate completionDate = testDate;

            given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
            given(dailyRecordRepository.findByMemberAndRecordDate(testMember, completionDate))
                    .willReturn(Optional.empty());

            // When & Then
            dashboardService.recordLearningCompletion(memberId, "word_study", completionDate);
            dashboardService.recordLearningCompletion(memberId, "Word_Study", completionDate);
            dashboardService.recordLearningCompletion(memberId, "SENTENCE_QUIZ", completionDate);
            dashboardService.recordLearningCompletion(memberId, "sentence_quiz", completionDate);

            verify(dailyRecordRepository, times(4)).save(any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원")
        void recordLearningCompletion_MemberNotFound_ThrowException() {
            // Given
            Long memberId = 999L;
            LocalDate completionDate = testDate;

            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    dashboardService.recordLearningCompletion(memberId, "WORD_STUDY", completionDate))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_MEMBER);

            verify(memberRepository, times(1)).findById(memberId);
            verify(dailyRecordRepository, never()).findByMemberAndRecordDate(any(), any());
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 learningType")
        void recordLearningCompletion_InvalidLearningType_ThrowException() {
            // Given
            Long memberId = 1L;
            LocalDate completionDate = testDate;
            String invalidType = "INVALID_TYPE";

            given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
            given(dailyRecordRepository.findByMemberAndRecordDate(testMember, completionDate))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    dashboardService.recordLearningCompletion(memberId, invalidType, completionDate))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ARGUMENT);

            verify(memberRepository, times(1)).findById(memberId);
            verify(dailyRecordRepository, times(1))
                    .findByMemberAndRecordDate(testMember, completionDate);
            verify(dailyRecordRepository, never()).save(any());
        }
    }

    // 테스트 헬퍼 메서드
    private DailyRecord createMockRecord(Member member, LocalDate date, boolean wordStudy, boolean sentenceQuiz) {
        DailyRecord record = DailyRecord.builder()
                .member(member)
                .recordDate(date)
                .build();
        if (wordStudy) {
            record.completeWordStudy();
        }
        if (sentenceQuiz) {
            record.completeSentenceQuiz();
        }
        return record;
    }
}
