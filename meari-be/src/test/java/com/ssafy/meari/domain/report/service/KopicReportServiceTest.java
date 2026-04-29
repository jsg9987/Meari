package com.ssafy.meari.domain.report.service;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.report.dto.response.KopicTotalReportListItemResponse;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.repository.KopicTotalReportRepository;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.common.CursorPageResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("KopicReportService 통합 테스트")
@Disabled("풀 컨텍스트 의존 — 환경 분리 후 재활성화")
class KopicReportServiceTest {

    @Autowired
    private KopicReportService kopicReportService;

    @Autowired
    private KopicTotalReportRepository kopicTotalReportRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Theme theme;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        member = memberRepository.save(Member.builder()
                .email("kopic-test@example.com")
                .nickname("코픽테스트유저")
                .password("password123")
                .build());

        theme = themeRepository.findById(1L)
                .orElseGet(() -> themeRepository.save(Theme.builder()
                        .name("공항")
                        .description("공항 관련 테마")
                        .themeUrl("https://example.com/theme/airport.png")
                        .build()));
    }

    @Nested
    @DisplayName("코픽 통합 리포트 목록 조회")
    class GetKopicTotalReportList {

        @Test
        @DisplayName("성공 - 첫 페이지 조회")
        void success_firstPage() {
            // Given: 20개의 COMPLETED 코픽 통합 리포트 생성
            for (int i = 0; i < 20; i++) {
                KopicTotalReport report = KopicTotalReport.builder()
                        .member(member)
                        .theme(theme)
                        .status(ReportStatus.PROCESSING)
                        .build();
                KopicTotalReport saved = kopicTotalReportRepository.save(report);

                // 분석 완료 처리
                int avgScore = 80 + (i % 15);
                saved.updateAggregation(avgScore, avgScore, 5, "[]");
            }

            entityManager.flush();

            // When: 첫 페이지 조회 (size=10, cursor=null)
            CursorPageResponse<KopicTotalReportListItemResponse> response =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), null, 10);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContents()).hasSize(10);
            assertThat(response.getSize()).isEqualTo(10);
            assertThat(response.isHasNext()).isTrue();
            assertThat(response.getNextCursor()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 커서 기반 페이징 (다음 페이지)")
        void success_withCursor() {
            // Given: 15개의 COMPLETED 리포트 생성
            List<KopicTotalReport> createdReports = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                KopicTotalReport report = KopicTotalReport.builder()
                        .member(member)
                        .theme(theme)
                        .status(ReportStatus.PROCESSING)
                        .build();
                KopicTotalReport saved = kopicTotalReportRepository.save(report);
                saved.updateAggregation(85, 85, 5, "[]");
                createdReports.add(saved);

                try { Thread.sleep(10); } catch (InterruptedException e) {}
            }

            entityManager.flush();

            // When: 첫 페이지 조회
            CursorPageResponse<KopicTotalReportListItemResponse> firstPage =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), null, 10);

            // 다음 페이지 조회
            LocalDateTime secondPageCursor = firstPage.getNextCursor() != null
                    ? LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(firstPage.getNextCursor()),
                            ZoneId.systemDefault())
                    : null;

            CursorPageResponse<KopicTotalReportListItemResponse> secondPage =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), secondPageCursor, 10);

            // Then
            assertThat(firstPage.getContents()).hasSize(10);
            assertThat(firstPage.isHasNext()).isTrue();

            assertThat(secondPage.getContents()).hasSize(5);
            assertThat(secondPage.isHasNext()).isFalse();
            assertThat(secondPage.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("성공 - 데이터 없음")
        void success_empty() {
            // Given: 리포트 없음

            // When: 목록 조회
            CursorPageResponse<KopicTotalReportListItemResponse> response =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), null, 10);

            // Then
            assertThat(response.getContents()).isEmpty();
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("성공 - 페이지 크기 경계값 (size=1)")
        void success_boundaryCase_minSize() {
            // Given: 5개의 COMPLETED 리포트 생성
            for (int i = 0; i < 5; i++) {
                KopicTotalReport report = KopicTotalReport.builder()
                        .member(member)
                        .theme(theme)
                        .status(ReportStatus.PROCESSING)
                        .build();
                KopicTotalReport saved = kopicTotalReportRepository.save(report);
                saved.updateAggregation(85, 85, 5, "[]");
            }

            entityManager.flush();

            // When: size=1로 조회
            CursorPageResponse<KopicTotalReportListItemResponse> response =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), null, 1);

            // Then
            assertThat(response.getContents()).hasSize(1);
            assertThat(response.isHasNext()).isTrue();
            assertThat(response.getNextCursor()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 페이지 크기 경계값 (size=정확한 개수)")
        void success_boundaryCase_exactSize() {
            // Given: 3개의 COMPLETED 리포트 생성
            for (int i = 0; i < 3; i++) {
                KopicTotalReport report = KopicTotalReport.builder()
                        .member(member)
                        .theme(theme)
                        .status(ReportStatus.PROCESSING)
                        .build();
                KopicTotalReport saved = kopicTotalReportRepository.save(report);
                saved.updateAggregation(85, 85, 5, "[]");
            }

            entityManager.flush();

            // When: size=3으로 조회
            CursorPageResponse<KopicTotalReportListItemResponse> response =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), null, 3);

            // Then: 정확히 3개만 반환되고 다음 페이지 없음
            assertThat(response.getContents()).hasSize(3);
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("성공 - COMPLETED 상태만 조회")
        void success_onlyCompletedStatus() {
            // Given: COMPLETED 리포트 3개 + PROCESSING 리포트 2개 생성
            for (int i = 0; i < 3; i++) {
                KopicTotalReport report = KopicTotalReport.builder()
                        .member(member)
                        .theme(theme)
                        .status(ReportStatus.PROCESSING)
                        .build();
                KopicTotalReport saved = kopicTotalReportRepository.save(report);
                saved.updateAggregation(85, 85, 5, "[]");
            }

            // PROCESSING 상태로 유지 (updateAggregation 호출 안 함)
            for (int i = 0; i < 2; i++) {
                KopicTotalReport report = KopicTotalReport.builder()
                        .member(member)
                        .theme(theme)
                        .status(ReportStatus.PROCESSING)
                        .build();
                kopicTotalReportRepository.save(report);
            }

            entityManager.flush();

            // When: 목록 조회
            CursorPageResponse<KopicTotalReportListItemResponse> response =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), null, 10);

            // Then: COMPLETED 상태의 3개만 반환
            assertThat(response.getContents()).hasSize(3);
            for (KopicTotalReportListItemResponse item : response.getContents()) {
                assertThat(item.getTotalScore()).isEqualTo(85);
            }
        }

        @Test
        @DisplayName("성공 - 최신순 정렬 (createdAt DESC)")
        void success_sortedByCreatedAtDesc() throws InterruptedException {
            // Given: 시간 간격을 두고 3개의 COMPLETED 리포트 생성
            for (int i = 0; i < 3; i++) {
                KopicTotalReport report = KopicTotalReport.builder()
                        .member(member)
                        .theme(theme)
                        .status(ReportStatus.PROCESSING)
                        .build();
                KopicTotalReport saved = kopicTotalReportRepository.save(report);
                saved.updateAggregation(80 + i, 80 + i, 5, "[]");
                Thread.sleep(10);
            }

            entityManager.flush();

            // When: 목록 조회
            CursorPageResponse<KopicTotalReportListItemResponse> response =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), null, 10);

            // Then: 최근 리포트가 먼저 반환 (DESC 정렬)
            assertThat(response.getContents()).hasSize(3);
            assertThat(response.getContents().get(0).getTotalScore()).isEqualTo(82);
            assertThat(response.getContents().get(1).getTotalScore()).isEqualTo(81);
            assertThat(response.getContents().get(2).getTotalScore()).isEqualTo(80);
        }

        @Test
        @DisplayName("성공 - DTO 필드 검증")
        void success_dtoFields() {
            // Given: 1개의 COMPLETED 리포트 생성
            KopicTotalReport report = KopicTotalReport.builder()
                    .member(member)
                    .theme(theme)
                    .status(ReportStatus.PROCESSING)
                    .build();
            KopicTotalReport saved = kopicTotalReportRepository.save(report);
            saved.updateAggregation(90, 90, 5, "[]");

            entityManager.flush();

            // When: 목록 조회
            CursorPageResponse<KopicTotalReportListItemResponse> response =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), null, 10);

            // Then: DTO 필드 검증
            assertThat(response.getContents()).hasSize(1);
            KopicTotalReportListItemResponse item = response.getContents().get(0);

            assertThat(item.getKopicTotalReportId()).isEqualTo(saved.getKopicTotalReportId());
            assertThat(item.getThemeId()).isEqualTo(theme.getThemeId());
            assertThat(item.getThemeName()).isEqualTo(theme.getName());
            assertThat(item.getThemeUrl()).isEqualTo(theme.getThemeUrl());
            assertThat(item.getTotalScore()).isEqualTo(90);
            assertThat(item.getIsRead()).isEqualTo(saved.getIsRead());
            assertThat(item.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공 - isRead 필드 확인")
        void success_isReadField() {
            // Given: 2개의 COMPLETED 리포트 생성 (isRead는 기본 false)
            for (int i = 0; i < 2; i++) {
                KopicTotalReport report = KopicTotalReport.builder()
                        .member(member)
                        .theme(theme)
                        .status(ReportStatus.PROCESSING)
                        .build();
                KopicTotalReport saved = kopicTotalReportRepository.save(report);
                saved.updateAggregation(85, 85, 5, "[]");
            }

            entityManager.flush();

            // When: 목록 조회
            CursorPageResponse<KopicTotalReportListItemResponse> response =
                    kopicReportService.getKopicTotalReportList(member.getMemberId(), null, 10);

            // Then: isRead는 기본적으로 false
            assertThat(response.getContents()).hasSize(2);
            for (KopicTotalReportListItemResponse item : response.getContents()) {
                assertThat(item.getIsRead()).isFalse();
            }
        }
    }
}
