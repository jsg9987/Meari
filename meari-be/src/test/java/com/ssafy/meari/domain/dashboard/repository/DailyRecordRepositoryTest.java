package com.ssafy.meari.domain.dashboard.repository;

import com.ssafy.meari.domain.dashboard.entity.DailyRecord;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스터")
                .nativeLanguage(NativeLanguage.KR)
                .build();
        testMember = memberRepository.save(testMember);
    }

    @AfterEach
    void tearDown() {
        dailyRecordRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("성공 - 특정 날짜 기록 조회")
    void findByMemberAndRecordDate_Success() {
        // Given
        LocalDate testDate = LocalDate.of(2025, 2, 2);
        DailyRecord record = createRecord(testMember, testDate, 2);

        // When
        Optional<DailyRecord> foundRecord = dailyRecordRepository
                .findByMemberAndRecordDate(testMember, testDate);

        // Then
        assertThat(foundRecord).isPresent();
        assertThat(foundRecord.get().getCompletedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("성공 - 기간별 완료 기록만 조회")
    void findCompletedRecordsByMemberAndDateRange_Success() {
        // Given
        LocalDate date1 = LocalDate.of(2025, 1, 5);
        LocalDate date2 = LocalDate.of(2025, 1, 10);
        LocalDate date3 = LocalDate.of(2025, 1, 15);

        createRecord(testMember, date1, 2);  // 완료
        createRecord(testMember, date2, 0);  // 미완료
        createRecord(testMember, date3, 1);  // 완료

        // When
        List<DailyRecord> records = dailyRecordRepository
                .findCompletedRecordsByMemberAndDateRange(
                    testMember,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 31)
                );

        // Then
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getCompletedCount()).isEqualTo(2);
        assertThat(records.get(1).getCompletedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("성공 - 전체 학습 일수 조회")
    void countCompletedDaysByMember_Success() {
        // Given
        createRecord(testMember, LocalDate.of(2025, 1, 1), 1);
        createRecord(testMember, LocalDate.of(2025, 1, 5), 2);
        createRecord(testMember, LocalDate.of(2025, 1, 10), 0);  // 미완료

        // When
        Long count = dailyRecordRepository.countCompletedDaysByMember(testMember);

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("성공 - incrementCompletedCount 도메인 메서드")
    void incrementCompletedCount_Success() {
        // Given
        DailyRecord record = DailyRecord.builder()
                .member(testMember)
                .recordDate(LocalDate.now())
                .build();

        // When
        record.incrementCompletedCount();
        record.incrementCompletedCount();
        DailyRecord savedRecord = dailyRecordRepository.save(record);

        // Then
        assertThat(savedRecord.getCompletedCount()).isEqualTo(2);
        assertThat(savedRecord.getLevel()).isEqualTo(2);
        assertThat(savedRecord.hasAnyCompletion()).isTrue();
    }

    // 테스트 헬퍼 메서드
    private DailyRecord createRecord(Member member, LocalDate date, int completedCount) {
        DailyRecord record = DailyRecord.builder()
                .member(member)
                .recordDate(date)
                .build();
        for (int i = 0; i < completedCount; i++) {
            record.incrementCompletedCount();
        }
        return dailyRecordRepository.save(record);
    }
}
