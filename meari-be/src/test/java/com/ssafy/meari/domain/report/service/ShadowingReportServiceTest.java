package com.ssafy.meari.domain.report.service;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.report.dto.response.ShadowingReportDetailResponse;
import com.ssafy.meari.domain.report.dto.response.ShadowingReportListItemResponse;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.KopicTotalReportRepository;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.common.CursorPageResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("ReportService 통합 테스트")
class ShadowingReportServiceTest {

    @Autowired
    private ShadowingReportService shadowingReportService;

    @Autowired
    private ShadowingReportRepository shadowingReportRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ThemeRepository themeRepository;

    private Member member;
    private Theme theme;
    private Room room;
    private Content content;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        member = memberRepository.save(Member.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .password("password123")
                .build());

        theme = themeRepository.findById(1L)
                .orElseGet(() -> themeRepository.save(Theme.builder()
                        .name("테스트테마")
                        .description("테스트 설명")
                        .themeUrl("/test.jpg")
                        .build()));

        room = roomRepository.save(Room.builder()
                .owner(member)
                .theme(theme)
                .title("테스트 방")
                .maxPeople(4)
                .build());

        content = contentRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException("Content ID 2가 존재하지 않습니다"));
    }

    @Test
    @DisplayName("쉐도잉 리포트 목록 조회 - 성공 케이스 (첫 페이지)")
    void getShadowingReportList_Success_FirstPage() {
        // Given: 20개의 쉐도잉 리포트 생성
        for (int i = 0; i < 20; i++) {
            Role role = roleRepository.findById(i % 2 == 0 ? 1L : 2L).orElseThrow();
            ShadowingReport report = ShadowingReport.builder()
                    .member(member)
                    .room(room)
                    .role(role)
                    .content(content)
                    .round(1)
                    .build();
            ShadowingReport saved = shadowingReportRepository.save(report);

            // 분석 결과 업데이트 (accuracy, intonation 설정)
            int score = 80 + (i % 15);
            saved.updateAnalysisResult(score, score, "{\"feedback\": \"테스트\"}");

            // is_read: 홀수 인덱스만 true
            if (i % 2 == 1) {
                saved.markAsRead();
            }
        }

        // When: 첫 페이지 조회 (size=10)
        CursorPageResponse<ShadowingReportListItemResponse> response = shadowingReportService.getShadowingReportList(
                member.getMemberId(), null, 10);

        // Then: 성공
        assertThat(response).isNotNull();
        assertThat(response.getContents()).hasSize(10);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getNextCursor()).isNotNull();

        // is_read 확인
        long readCount = response.getContents().stream()
                .filter(ShadowingReportListItemResponse::getIsRead)
                .count();
        assertThat(readCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("쉐도잉 리포트 목록 조회 - 커서 기반 페이징")
    void getShadowingReportList_Success_WithCursor() {
        // Given: 15개의 리포트 생성 (의도적으로 시간차 두고 생성)
        System.out.println("\n========== 데이터 생성 ==========");
        java.util.List<ShadowingReport> createdReports = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Role role = roleRepository.findById(i % 2 == 0 ? 1L : 2L).orElseThrow();
            ShadowingReport report = ShadowingReport.builder()
                    .member(member)
                    .room(room)
                    .role(role)
                    .content(content)
                    .round(1)
                    .build();
            ShadowingReport saved = shadowingReportRepository.save(report);
            saved.updateAnalysisResult(85, 85, "{\"feedback\": \"테스트\"}");
            createdReports.add(saved);

            // 각 리포트의 생성 시간 출력
            System.out.printf("생성 리포트 #%2d | ID: %3d | created_at: %s%n",
                    i + 1, saved.getShadowingReportId(), saved.getCreatedAt());

            // 약간의 시간차를 위해 sleep (선택사항)
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        }

        // When: 첫 페이지 조회
        System.out.println("\n========== 첫 페이지 조회 (size=10, cursor=null) ==========");
        CursorPageResponse<ShadowingReportListItemResponse> firstPage = shadowingReportService.getShadowingReportList(
                member.getMemberId(), null, 10);

        System.out.println("조회 결과:");
        java.util.List<ShadowingReportListItemResponse> firstPageContents = firstPage.getContents();
        for (int i = 0; i < firstPageContents.size(); i++) {
            ShadowingReportListItemResponse report = firstPageContents.get(i);
            System.out.printf("  [%2d] ID: %4d | created_at: %s | is_read: %s%n",
                    i + 1, report.getShadowingReportId(),
                    report.getCreatedAt(), report.getIsRead());
        }
        System.out.printf("반환된 커서 (timestamp ms): %s%n", firstPage.getNextCursor());
        System.out.printf("다음 페이지 존재: %s%n", firstPage.isHasNext());

        // 다음 페이지 조회 (nextCursor 변환: Long timestamp → LocalDateTime)
        System.out.println("\n========== 두 번째 페이지 조회 ==========");
        LocalDateTime secondPageCursor = firstPage.getNextCursor() != null
                ? LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(firstPage.getNextCursor()),
                        ZoneId.systemDefault())
                : null;

        System.out.printf("전달된 커서 (timestamp): %s%n", firstPage.getNextCursor());
        System.out.printf("변환된 커서 (LocalDateTime): %s%n", secondPageCursor);

        CursorPageResponse<ShadowingReportListItemResponse> secondPage = shadowingReportService.getShadowingReportList(
                member.getMemberId(), secondPageCursor, 10);

        System.out.println("조회 결과:");
        java.util.List<ShadowingReportListItemResponse> secondPageContents = secondPage.getContents();
        for (int i = 0; i < secondPageContents.size(); i++) {
            ShadowingReportListItemResponse report = secondPageContents.get(i);
            System.out.printf("  [%2d] ID: %4d | created_at: %s | is_read: %s%n",
                    i + 1, report.getShadowingReportId(),
                    report.getCreatedAt(), report.getIsRead());
        }
        System.out.printf("반환된 커서: %s%n", secondPage.getNextCursor());
        System.out.printf("다음 페이지 존재: %s%n", secondPage.isHasNext());

        // 정렬 순서 검증
        System.out.println("\n========== 정렬 순서 검증 ==========");
        java.util.List<LocalDateTime> firstPageTimes = firstPage.getContents().stream()
                .map(ShadowingReportListItemResponse::getCreatedAt)
                .toList();
        java.util.List<LocalDateTime> secondPageTimes = secondPage.getContents().stream()
                .map(ShadowingReportListItemResponse::getCreatedAt)
                .toList();

        // 첫 페이지 내림차순 정렬 검증 (각 항목이 이전 항목보다 이전 시간인지 확인)
        boolean firstPageSorted = true;
        for (int i = 1; i < firstPageTimes.size(); i++) {
            if (!firstPageTimes.get(i).isBefore(firstPageTimes.get(i - 1)) &&
                !firstPageTimes.get(i).isEqual(firstPageTimes.get(i - 1))) {
                firstPageSorted = false;
                break;
            }
        }
        System.out.printf("첫 페이지 내림차순 정렬: %s%n", firstPageSorted);

        // 두 번째 페이지 내림차순 정렬 검증
        boolean secondPageSorted = true;
        for (int i = 1; i < secondPageTimes.size(); i++) {
            if (!secondPageTimes.get(i).isBefore(secondPageTimes.get(i - 1)) &&
                !secondPageTimes.get(i).isEqual(secondPageTimes.get(i - 1))) {
                secondPageSorted = false;
                break;
            }
        }
        System.out.printf("두 번째 페이지 내림차순 정렬: %s%n", secondPageSorted);

        // Then
        assertThat(firstPage.getContents()).hasSize(10);
        assertThat(secondPage.getContents()).hasSize(5);
        assertThat(secondPage.isHasNext()).isFalse();
        assertThat(secondPage.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("쉐도잉 리포트 목록 조회 - 데이터 없음")
    void getShadowingReportList_Empty() {
        // When: 리포트가 없는 사용자의 목록 조회
        CursorPageResponse<ShadowingReportListItemResponse> response = shadowingReportService.getShadowingReportList(
                member.getMemberId(), null, 10);

        // Then
        assertThat(response.getContents()).isEmpty();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("쉐도잉 리포트 상세 조회 - 성공 및 자동 읽음 처리")
    void getShadowingReportDetail_Success() {
        // Given: 쉐도잉 리포트 생성 (is_read = false)
        Role role = roleRepository.findById(1L).orElseThrow();
        ShadowingReport report = ShadowingReport.builder()
                .member(member)
                .room(room)
                .role(role)
                .content(content)
                .round(1)
                .build();
        ShadowingReport savedReport = shadowingReportRepository.save(report);
        savedReport.updateAnalysisResult(85, 80, "{\"feedback\": \"좋은 발음입니다\"}");
        assertThat(savedReport.getIsRead()).isFalse();

        // When: 상세 조회
        ShadowingReportDetailResponse response = shadowingReportService.getShadowingReportDetail(
                savedReport.getShadowingReportId(), member.getMemberId());

        // Then: 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.getShadowingReportId()).isEqualTo(savedReport.getShadowingReportId());
        assertThat(response.getAccuracy()).isEqualTo(85);
        assertThat(response.getIntonation()).isEqualTo(80);

        // Then: 자동으로 is_read가 true로 변경됨
        ShadowingReport updated = shadowingReportRepository.findById(savedReport.getShadowingReportId())
                .orElseThrow();
        assertThat(updated.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("쉐도잉 리포트 상세 조회 - 리포트 없음 (실패)")
    void getShadowingReportDetail_NotFound() {
        // When & Then: 존재하지 않는 리포트 조회 시 예외 발생
        assertThatThrownBy(() -> shadowingReportService.getShadowingReportDetail(9999L, member.getMemberId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_FOUND_SHADOWING_REPORT.getMessage());
    }

    @Test
    @DisplayName("쉐도잉 리포트 상세 조회 - 권한 없음 (실패)")
    void getShadowingReportDetail_Forbidden() {
        // Given: 다른 사용자의 리포트
        Member otherMember = Member.builder()
                .email("other@example.com")
                .nickname("다른유저")
                .password("password123")
                .build();
        memberRepository.save(otherMember);

        Role role = roleRepository.findById(1L).orElseThrow();
        ShadowingReport report = ShadowingReport.builder()
                .member(otherMember)
                .room(room)
                .role(role)
                .content(content)
                .round(1)
                .build();
        ShadowingReport savedReport = shadowingReportRepository.save(report);
        savedReport.updateAnalysisResult(85, 80, "{\"feedback\": \"테스트\"}");

        // When & Then: 다른 사용자가 접근 시 예외 발생
        assertThatThrownBy(() -> shadowingReportService.getShadowingReportDetail(
                savedReport.getShadowingReportId(), member.getMemberId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

}
