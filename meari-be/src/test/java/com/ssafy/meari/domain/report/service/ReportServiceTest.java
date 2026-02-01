package com.ssafy.meari.domain.report.service;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.report.dto.response.KopicReportListResponse;
import com.ssafy.meari.domain.report.dto.response.ShadowingReportListResponse;
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
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ShadowingReportRepository shadowingReportRepository;

    @Autowired
    private KopicTotalReportRepository kopicTotalReportRepository;

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
        CursorPageResponse<ShadowingReportListResponse> response = reportService.getShadowingReportList(
                member.getMemberId(), null, 10);

        // Then: 성공
        assertThat(response).isNotNull();
        assertThat(response.getContents()).hasSize(10);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getNextCursor()).isNotNull();

        // is_read 확인
        long readCount = response.getContents().stream()
                .filter(ShadowingReportListResponse::getIsRead)
                .count();
        assertThat(readCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("쉐도잉 리포트 목록 조회 - 커서 기반 페이징")
    void getShadowingReportList_Success_WithCursor() {
        // Given: 15개의 리포트 생성
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
        }

        // When: 첫 페이지 조회
        CursorPageResponse<ShadowingReportListResponse> firstPage = reportService.getShadowingReportList(
                member.getMemberId(), null, 10);

        // 다음 페이지 조회 (nextCursor 변환: Long timestamp → LocalDateTime)
        LocalDateTime secondPageCursor = firstPage.getNextCursor() != null
                ? LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(firstPage.getNextCursor()),
                        ZoneId.systemDefault())
                : null;
        CursorPageResponse<ShadowingReportListResponse> secondPage = reportService.getShadowingReportList(
                member.getMemberId(), secondPageCursor, 10);

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
        CursorPageResponse<ShadowingReportListResponse> response = reportService.getShadowingReportList(
                member.getMemberId(), null, 10);

        // Then
        assertThat(response.getContents()).isEmpty();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("코픽 리포트 목록 조회 - 성공 케이스")
    void getKopicReportList_Success() {
        // Given: 15개의 코픽 리포트 생성
        for (int i = 0; i < 15; i++) {
            KopicTotalReport report = KopicTotalReport.builder()
                    .member(member)
                    .theme(theme)
                    .status(ReportStatus.COMPLETED)
                    .build();
            KopicTotalReport saved = kopicTotalReportRepository.save(report);

            // is_read: 짝수 인덱스만 true
            if (i % 2 == 0) {
                saved.markAsRead();
            }
        }

        // When: 첫 페이지 조회 (size=10)
        CursorPageResponse<KopicReportListResponse> response = reportService.getKopicReportList(
                member.getMemberId(), null, 10);

        // Then
        assertThat(response.getContents()).hasSize(10);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getNextCursor()).isNotNull();

        // is_read 확인
        long readCount = response.getContents().stream()
                .filter(KopicReportListResponse::getIsRead)
                .count();
        assertThat(readCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("코픽 리포트 목록 조회 - 데이터 없음")
    void getKopicReportList_Empty() {
        // When: 리포트가 없는 사용자의 목록 조회
        CursorPageResponse<KopicReportListResponse> response = reportService.getKopicReportList(
                member.getMemberId(), null, 10);

        // Then
        assertThat(response.getContents()).isEmpty();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("쉐도잉 리포트 읽음 처리 - 성공")
    void markShadowingReportAsRead_Success() {
        // Given: 쉐도잉 리포트 생성
        Role role = roleRepository.findById(1L).orElseThrow();
        ShadowingReport report = ShadowingReport.builder()
                .member(member)
                .room(room)
                .role(role)
                .content(content)
                .round(1)
                .build();
        ShadowingReport savedReport = shadowingReportRepository.save(report);
        savedReport.updateAnalysisResult(85, 85, "{\"feedback\": \"테스트\"}");
        assertThat(savedReport.getIsRead()).isFalse();

        // When: 읽음 처리
        reportService.markShadowingReportAsRead(savedReport.getShadowingReportId(), member.getMemberId());

        // Then: is_read가 true로 변경됨
        ShadowingReport updated = shadowingReportRepository.findById(savedReport.getShadowingReportId())
                .orElseThrow();
        assertThat(updated.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("쉐도잉 리포트 읽음 처리 - 리포트 없음 (실패)")
    void markShadowingReportAsRead_NotFound() {
        // When & Then: 존재하지 않는 리포트 조회 시 예외 발생
        assertThatThrownBy(() -> reportService.markShadowingReportAsRead(9999L, member.getMemberId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_FOUND_SHADOWING_REPORT.getMessage());
    }

    @Test
    @DisplayName("쉐도잉 리포트 읽음 처리 - 권한 없음 (실패)")
    void markShadowingReportAsRead_Forbidden() {
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
        savedReport.updateAnalysisResult(85, 85, "{\"feedback\": \"테스트\"}");


        // When & Then: 다른 사용자가 접근 시 예외 발생
        assertThatThrownBy(() -> reportService.markShadowingReportAsRead(
                savedReport.getShadowingReportId(), member.getMemberId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("코픽 리포트 읽음 처리 - 성공")
    void markKopicReportAsRead_Success() {
        // Given: 코픽 리포트 생성
        KopicTotalReport report = KopicTotalReport.builder()
                .member(member)
                .theme(theme)
                .status(ReportStatus.COMPLETED)
                .build();
        KopicTotalReport savedReport = kopicTotalReportRepository.save(report);
        assertThat(savedReport.getIsRead()).isFalse();

        // When: 읽음 처리
        reportService.markKopicReportAsRead(savedReport.getKopicTotalReportId(), member.getMemberId());

        // Then: is_read가 true로 변경됨
        KopicTotalReport updated = kopicTotalReportRepository.findById(savedReport.getKopicTotalReportId())
                .orElseThrow();
        assertThat(updated.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("코픽 리포트 읽음 처리 - 리포트 없음 (실패)")
    void markKopicReportAsRead_NotFound() {
        // When & Then: 존재하지 않는 리포트 조회 시 예외 발생
        assertThatThrownBy(() -> reportService.markKopicReportAsRead(9999L, member.getMemberId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_FOUND_KOPIC_TOTAL_REPORT.getMessage());
    }

    @Test
    @DisplayName("코픽 리포트 읽음 처리 - 권한 없음 (실패)")
    void markKopicReportAsRead_Forbidden() {
        // Given: 다른 사용자의 리포트
        Member otherMember = Member.builder()
                .email("other@example.com")
                .nickname("다른유저")
                .password("password123")
                .build();
        memberRepository.save(otherMember);

        KopicTotalReport report = KopicTotalReport.builder()
                .member(otherMember)
                .theme(theme)
                .status(ReportStatus.COMPLETED)
                .build();
        KopicTotalReport savedReport = kopicTotalReportRepository.save(report);

        // When & Then: 다른 사용자가 접근 시 예외 발생
        assertThatThrownBy(() -> reportService.markKopicReportAsRead(
                savedReport.getKopicTotalReportId(), member.getMemberId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }
}
