package com.ssafy.meari.domain.dashboard.repository;

import com.ssafy.meari.domain.dashboard.entity.DailyRecord;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("DailyRecordRepository 통합 테스트")
class DailyRecordRepositoryTest {

    @Autowired
    private DailyRecordRepository dailyRecordRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = Member.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스터")
                .nativeLanguage(NativeLanguage.KR)
                .build();
        testMember = memberRepository.save(testMember);

        testDate = LocalDate.of(2025, 2, 2);
    }

    @AfterEach
    void tearDown() {
        dailyRecordRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Nested
    @DisplayName("특정 날짜 기록 조회")
    class FindByMemberAndRecordDate {

        @Test
        @DisplayName("성공 - 특정 날짜 기록 조회")
        void findByMemberAndRecordDate_Success() {
            // Given
            DailyRecord record = DailyRecord.builder()
                    .member(testMember)
                    .recordDate(testDate)
                    .build();
            record.completeWordStudy();
            dailyRecordRepository.save(record);

            // When
            Optional<DailyRecord> foundRecord = dailyRecordRepository
                    .findByMemberAndRecordDate(testMember, testDate);

            // Then
            assertThat(foundRecord).isPresent();
            assertThat(foundRecord.get().getRecordDate()).isEqualTo(testDate);
            assertThat(foundRecord.get().getIsWordStudyFinished()).isTrue();
            assertThat(foundRecord.get().getIsSentenceQuizFinished()).isFalse();
        }

        @Test
        @DisplayName("성공 - 기록 없을 때 빈 Optional 반환")
        void findByMemberAndRecordDate_EmptyOptional() {
            // Given
            LocalDate noRecordDate = LocalDate.of(2025, 1, 1);

            // When
            Optional<DailyRecord> foundRecord = dailyRecordRepository
                    .findByMemberAndRecordDate(testMember, noRecordDate);

            // Then
            assertThat(foundRecord).isEmpty();
        }
    }

    @Nested
    @DisplayName("기간별 기록 조회")
    class FindByMemberAndDateRange {

        @Test
        @DisplayName("성공 - 주간 기록 조회 (월요일~일요일)")
        void findByMemberAndDateRange_Weekly_Success() {
            // Given
            LocalDate monday = LocalDate.of(2025, 1, 27).with(DayOfWeek.MONDAY);
            LocalDate wednesday = monday.plusDays(2);
            LocalDate friday = monday.plusDays(4);
            LocalDate sunday = monday.with(DayOfWeek.SUNDAY);

            createRecord(testMember, wednesday, true, false);
            createRecord(testMember, friday, true, true);

            // When
            List<DailyRecord> records = dailyRecordRepository
                    .findByMemberAndDateRange(testMember, monday, sunday);

            // Then
            assertThat(records).hasSize(2);
            assertThat(records.get(0).getRecordDate()).isEqualTo(wednesday);
            assertThat(records.get(1).getRecordDate()).isEqualTo(friday);
        }

        @Test
        @DisplayName("성공 - 월간 기록 조회 (1일~마지막날)")
        void findByMemberAndDateRange_Monthly_Success() {
            // Given
            LocalDate jan1 = LocalDate.of(2025, 1, 1);
            LocalDate jan15 = LocalDate.of(2025, 1, 15);
            LocalDate jan31 = LocalDate.of(2025, 1, 31);

            createRecord(testMember, jan1, true, false);
            createRecord(testMember, jan15, true, true);
            createRecord(testMember, jan31, false, true);

            // When
            List<DailyRecord> records = dailyRecordRepository
                    .findByMemberAndDateRange(testMember, jan1, jan31);

            // Then
            assertThat(records).hasSize(3);
            assertThat(records.get(0).getRecordDate()).isEqualTo(jan1);
            assertThat(records.get(1).getRecordDate()).isEqualTo(jan15);
            assertThat(records.get(2).getRecordDate()).isEqualTo(jan31);
        }

        @Test
        @DisplayName("성공 - 기간 내 기록 없을 때 빈 리스트 반환")
        void findByMemberAndDateRange_EmptyList() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 3, 1);
            LocalDate endDate = LocalDate.of(2025, 3, 31);

            // When
            List<DailyRecord> records = dailyRecordRepository
                    .findByMemberAndDateRange(testMember, startDate, endDate);

            // Then
            assertThat(records).isEmpty();
        }

        @Test
        @DisplayName("성공 - 날짜 오름차순 정렬")
        void findByMemberAndDateRange_OrderByDateAsc() {
            // Given
            LocalDate date1 = LocalDate.of(2025, 2, 1);
            LocalDate date2 = LocalDate.of(2025, 2, 5);
            LocalDate date3 = LocalDate.of(2025, 2, 10);

            createRecord(testMember, date3, true, false); // 나중에 저장
            createRecord(testMember, date1, true, false); // 먼저 저장
            createRecord(testMember, date2, true, false);

            // When
            List<DailyRecord> records = dailyRecordRepository
                    .findByMemberAndDateRange(testMember, date1, date3);

            // Then
            assertThat(records).hasSize(3);
            assertThat(records.get(0).getRecordDate()).isEqualTo(date1);
            assertThat(records.get(1).getRecordDate()).isEqualTo(date2);
            assertThat(records.get(2).getRecordDate()).isEqualTo(date3);
        }
    }

    @Nested
    @DisplayName("학습 완료한 기록만 조회")
    class FindCompletedRecordsByMemberAndDateRange {

        @Test
        @DisplayName("성공 - 학습 완료한 날짜만 조회")
        void findCompletedRecordsByMemberAndDateRange_Success() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 31);

            createRecord(testMember, LocalDate.of(2025, 1, 5), true, false);  // 완료
            createRecord(testMember, LocalDate.of(2025, 1, 10), false, true); // 완료
            createRecord(testMember, LocalDate.of(2025, 1, 15), true, true);  // 완료
            createRecord(testMember, LocalDate.of(2025, 1, 20), false, false); // 미완료

            // When
            List<DailyRecord> records = dailyRecordRepository
                    .findCompletedRecordsByMemberAndDateRange(testMember, startDate, endDate);

            // Then
            assertThat(records).hasSize(3);
            assertThat(records.get(0).getRecordDate()).isEqualTo(LocalDate.of(2025, 1, 5));
            assertThat(records.get(1).getRecordDate()).isEqualTo(LocalDate.of(2025, 1, 10));
            assertThat(records.get(2).getRecordDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        }

        @Test
        @DisplayName("성공 - 단어 학습만 완료한 기록 포함")
        void findCompletedRecordsByMemberAndDateRange_WordStudyOnly() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 31);

            createRecord(testMember, LocalDate.of(2025, 1, 5), true, false);

            // When
            List<DailyRecord> records = dailyRecordRepository
                    .findCompletedRecordsByMemberAndDateRange(testMember, startDate, endDate);

            // Then
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getIsWordStudyFinished()).isTrue();
            assertThat(records.get(0).getIsSentenceQuizFinished()).isFalse();
        }

        @Test
        @DisplayName("성공 - 문장 퀴즈만 완료한 기록 포함")
        void findCompletedRecordsByMemberAndDateRange_SentenceQuizOnly() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 31);

            createRecord(testMember, LocalDate.of(2025, 1, 5), false, true);

            // When
            List<DailyRecord> records = dailyRecordRepository
                    .findCompletedRecordsByMemberAndDateRange(testMember, startDate, endDate);

            // Then
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getIsWordStudyFinished()).isFalse();
            assertThat(records.get(0).getIsSentenceQuizFinished()).isTrue();
        }

        @Test
        @DisplayName("성공 - 미완료 기록은 제외")
        void findCompletedRecordsByMemberAndDateRange_ExcludeNotCompleted() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 31);

            createRecord(testMember, LocalDate.of(2025, 1, 5), false, false); // 미완료

            // When
            List<DailyRecord> records = dailyRecordRepository
                    .findCompletedRecordsByMemberAndDateRange(testMember, startDate, endDate);

            // Then
            assertThat(records).isEmpty();
        }
    }

    @Nested
    @DisplayName("전체 학습 일수 조회")
    class CountCompletedDaysByMember {

        @Test
        @DisplayName("성공 - 전체 학습 일수 조회")
        void countCompletedDaysByMember_Success() {
            // Given
            createRecord(testMember, LocalDate.of(2025, 1, 1), true, false);
            createRecord(testMember, LocalDate.of(2025, 1, 5), true, true);
            createRecord(testMember, LocalDate.of(2025, 1, 10), false, true);
            createRecord(testMember, LocalDate.of(2025, 1, 15), false, false); // 미완료

            // When
            Long count = dailyRecordRepository.countCompletedDaysByMember(testMember);

            // Then
            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("성공 - 학습 기록 없을 때 0 반환")
        void countCompletedDaysByMember_Zero() {
            // Given
            // 기록 없음

            // When
            Long count = dailyRecordRepository.countCompletedDaysByMember(testMember);

            // Then
            assertThat(count).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("도메인 메서드 테스트")
    class DomainMethods {

        @Test
        @DisplayName("성공 - 단어 학습 완료")
        void completeWordStudy_Success() {
            // Given
            DailyRecord record = DailyRecord.builder()
                    .member(testMember)
                    .recordDate(testDate)
                    .build();

            // When
            record.completeWordStudy();
            DailyRecord savedRecord = dailyRecordRepository.save(record);

            // Then
            assertThat(savedRecord.getIsWordStudyFinished()).isTrue();
            assertThat(savedRecord.getIsSentenceQuizFinished()).isFalse();
            assertThat(savedRecord.getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 문장 퀴즈 완료")
        void completeSentenceQuiz_Success() {
            // Given
            DailyRecord record = DailyRecord.builder()
                    .member(testMember)
                    .recordDate(testDate)
                    .build();

            // When
            record.completeSentenceQuiz();
            DailyRecord savedRecord = dailyRecordRepository.save(record);

            // Then
            assertThat(savedRecord.getIsWordStudyFinished()).isFalse();
            assertThat(savedRecord.getIsSentenceQuizFinished()).isTrue();
            assertThat(savedRecord.getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 두 학습 모두 완료 시 level 2")
        void bothCompleted_LevelIsTwo() {
            // Given
            DailyRecord record = DailyRecord.builder()
                    .member(testMember)
                    .recordDate(testDate)
                    .build();

            // When
            record.completeWordStudy();
            record.completeSentenceQuiz();
            DailyRecord savedRecord = dailyRecordRepository.save(record);

            // Then
            assertThat(savedRecord.getIsWordStudyFinished()).isTrue();
            assertThat(savedRecord.getIsSentenceQuizFinished()).isTrue();
            assertThat(savedRecord.getLevel()).isEqualTo(2);
            assertThat(savedRecord.hasAnyCompletion()).isTrue();
        }

        @Test
        @DisplayName("성공 - 미완료 시 level 0")
        void notCompleted_LevelIsZero() {
            // Given
            DailyRecord record = DailyRecord.builder()
                    .member(testMember)
                    .recordDate(testDate)
                    .build();

            // When
            DailyRecord savedRecord = dailyRecordRepository.save(record);

            // Then
            assertThat(savedRecord.getIsWordStudyFinished()).isFalse();
            assertThat(savedRecord.getIsSentenceQuizFinished()).isFalse();
            assertThat(savedRecord.getLevel()).isEqualTo(0);
            assertThat(savedRecord.hasAnyCompletion()).isFalse();
        }
    }

    // 테스트 헬퍼 메서드
    private void createRecord(Member member, LocalDate date, boolean wordStudy, boolean sentenceQuiz) {
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
        dailyRecordRepository.save(record);
    }
}
