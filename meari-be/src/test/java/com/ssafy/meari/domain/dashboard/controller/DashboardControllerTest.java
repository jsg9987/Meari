package com.ssafy.meari.domain.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.auth.jwt.JwtUtil;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Dashboard 쉐도잉 이력 조회 통합 테스트")
@Disabled("DB unique 충돌 — 테스트 격리 후 재활성화")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtUtil jwtUtil;

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
    private Role role;
    private String validToken;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        member = memberRepository.save(Member.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .password("password123")
                .build());

        // UserDetailsImpl 객체 생성하여 토큰 생성
        UserDetailsImpl userDetails = new UserDetailsImpl(member);
        validToken = jwtUtil.generateAccessToken(userDetails);

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

        content = contentRepository.findById(1L)
                .orElseGet(() -> contentRepository.save(Content.builder()
                        .theme(theme)
                        .title("테스트 콘텐츠")
                        .videoUrl("http://example.com/video.mp4")
                        .thumbnailUrl("http://example.com/thumbnail.png")
                        .maxPeople(4)
                        .totalDuration(new java.math.BigDecimal("120.500"))
                        .build()));

        // Role이 없으면 생성 (테스트 데이터용)
        // ⚠️ Role은 Content와 @ManyToOne 관계이므로 content 필수
        role = roleRepository.findById(1L)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .content(content)
                        .name("학생 1")
                        .build()));

        // 영속성 컨텍스트 플러시: setUp 단계의 모든 데이터를 DB에 반영
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("최근 5회 연습 이력 조회 - 정상 요청 (200 OK)")
    void getRecentPracticeHistory_Success() throws Exception {
        // Given: 3개의 COMPLETED 리포트 생성
        for (int i = 0; i < 3; i++) {
            ShadowingReport report = ShadowingReport.builder()
                    .member(member)
                    .room(room)
                    .role(role)  // setUp()에서 생성한 멤버 변수 사용
                    .content(content)
                    .round(1 + i)  // round를 다르게 설정 (UNIQUE 제약조건)
                    .build();
            ShadowingReport saved = shadowingReportRepository.save(report);
            saved.updateAnalysisResult(80 + i, 80 + i, "{}");
        }

        // 데이터 플러시: 영속성 컨텍스트의 데이터를 DB에 반영
        entityManager.flush();
        entityManager.clear();  // 영속성 컨텍스트 클리어 (실제 DB 조회 강제)

        // When & Then: GET /api/v1/dashboard/me/shadowing
        mockMvc.perform(get("/api/v1/dashboard/me/shadowing")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("최근 5회 연습 이력 조회 - 응답 필드 존재 검증")
    void getRecentPracticeHistory_ResponseFields() throws Exception {
        // Given: 1개의 COMPLETED 리포트 생성
        ShadowingReport report = ShadowingReport.builder()
                .member(member)
                .room(room)
                .role(role)  // setUp()에서 생성한 멤버 변수 사용
                .content(content)
                .round(1)
                .build();
        ShadowingReport saved = shadowingReportRepository.save(report);
        saved.updateAnalysisResult(85, 90, "{\"sentences\": []}");

        // 데이터 플러시
        entityManager.flush();
        entityManager.clear();

        // When & Then: 응답 필드 검증
        mockMvc.perform(get("/api/v1/dashboard/me/shadowing")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].idx").exists())
                .andExpect(jsonPath("$.data[0].idx", notNullValue()))
                .andExpect(jsonPath("$.data[0].date").exists())
                .andExpect(jsonPath("$.data[0].date", notNullValue()))
                .andExpect(jsonPath("$.data[0].accuracy").exists())
                .andExpect(jsonPath("$.data[0].accuracy", notNullValue()))
                .andExpect(jsonPath("$.data[0].intonation").exists())
                .andExpect(jsonPath("$.data[0].intonation", notNullValue()))
                .andExpect(jsonPath("$.data[0].errors_count").exists())
                .andExpect(jsonPath("$.data[0].errors_count", notNullValue()));
    }

    @Test
    @DisplayName("최근 5회 연습 이력 조회 - 리포트 없음 (200 OK + 빈 배열)")
    void getRecentPracticeHistory_NoReports() throws Exception {
        // Given: 리포트 없음 (member만 존재)

        // When & Then: GET /api/v1/dashboard/me/shadowing
        mockMvc.perform(get("/api/v1/dashboard/me/shadowing")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("최근 5회 연습 이력 조회 - 인증 토큰 없음 (400 Bad Request)")
    void getRecentPracticeHistory_NoToken() throws Exception {
        // When & Then: 토큰 없이 요청 - 헤더 형식 오류로 400 반환
        mockMvc.perform(get("/api/v1/dashboard/me/shadowing")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("최근 5회 연습 이력 조회 - 잘못된 토큰 (401 Unauthorized)")
    void getRecentPracticeHistory_InvalidToken() throws Exception {
        // When & Then: 잘못된 토큰으로 요청
        mockMvc.perform(get("/api/v1/dashboard/me/shadowing")
                .header("Authorization", "Bearer invalid_token_here")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("최근 5회 연습 이력 조회 - ApiResponse 구조 검증")
    void getRecentPracticeHistory_ApiResponseStructure() throws Exception {
        // Given: 2개의 COMPLETED 리포트 생성
        for (int i = 0; i < 2; i++) {
            ShadowingReport report = ShadowingReport.builder()
                    .member(member)
                    .room(room)
                    .role(role)  // setUp()에서 생성한 멤버 변수 사용
                    .content(content)
                    .round(1 + i)  // round를 다르게 설정 (UNIQUE 제약조건)
                    .build();
            ShadowingReport saved = shadowingReportRepository.save(report);
            saved.updateAnalysisResult(85, 85, "{}");
        }

        // 데이터 플러시
        entityManager.flush();
        entityManager.clear();

        // When & Then: ApiResponse 구조 검증
        mockMvc.perform(get("/api/v1/dashboard/me/shadowing")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // ApiResponse 최상위 필드
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)))
                .andExpect(jsonPath("$.error").value(nullValue()))
                // 응답 데이터 구조
                .andExpect(jsonPath("$", hasKey("success")))
                .andExpect(jsonPath("$", hasKey("data")))
                .andExpect(jsonPath("$", hasKey("error")));
    }

    @Test
    @DisplayName("최근 5회 연습 이력 조회 - 데이터 정확성 검증")
    void getRecentPracticeHistory_DataAccuracy() throws Exception {
        // Given: 정확한 값의 리포트 생성
        ShadowingReport report = ShadowingReport.builder()
                .member(member)
                .room(room)
                .role(role)  // setUp()에서 생성한 멤버 변수 사용
                .content(content)
                .round(1)
                .build();
        ShadowingReport saved = shadowingReportRepository.save(report);
        saved.updateAnalysisResult(87, 92, "{\"sentences\": []}");

        // 데이터 플러시
        entityManager.flush();
        entityManager.clear();

        // When & Then: 정확한 값 검증
        mockMvc.perform(get("/api/v1/dashboard/me/shadowing")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].idx").value(1))
                .andExpect(jsonPath("$.data[0].accuracy").value(87))
                .andExpect(jsonPath("$.data[0].intonation").value(92))
                .andExpect(jsonPath("$.data[0].errors_count").value(0))
                .andExpect(jsonPath("$.data[0].date").exists());
    }

}
