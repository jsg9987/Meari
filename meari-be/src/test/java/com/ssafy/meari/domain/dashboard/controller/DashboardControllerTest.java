package com.ssafy.meari.domain.dashboard.controller;

import com.ssafy.meari.domain.dashboard.dto.response.ActivityResponse;
import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;
import com.ssafy.meari.domain.dashboard.service.DashboardService;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@DisplayName("DashboardController 단위 테스트")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    private UserDetailsImpl userDetails;
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
        // Reflection으로 memberId 설정
        org.springframework.test.util.ReflectionTestUtils.setField(testMember, "memberId", 1L);

        userDetails = new UserDetailsImpl(testMember);
        testDate = LocalDate.of(2025, 2, 2);
    }

    @Nested
    @DisplayName("GET /api/v1/dashboard/me/daily-records")
    class GetDailyRecords {

        @Test
        @WithMockUser
        @DisplayName("성공 - 주간 기록 조회")
        void getDailyRecords_Weekly_Success() throws Exception {
            // Given
            LocalDate monday = testDate.with(DayOfWeek.MONDAY);
            LocalDate sunday = testDate.with(DayOfWeek.SUNDAY);

            DailyRecordsResponse mockResponse = DailyRecordsResponse.builder()
                    .startDate(monday.toString())
                    .endDate(sunday.toString())
                    .activities(Arrays.asList(
                            ActivityResponse.builder()
                                    .date("2025-01-29")
                                    .level(2)
                                    .wordStudy(true)
                                    .sentenceQuiz(true)
                                    .build(),
                            ActivityResponse.builder()
                                    .date("2025-01-31")
                                    .level(1)
                                    .wordStudy(true)
                                    .sentenceQuiz(false)
                                    .build()
                    ))
                    .build();

            given(dashboardService.getDailyRecords(eq(1L), eq("weekly"), any(LocalDate.class)))
                    .willReturn(mockResponse);

            // When
            ResultActions result = mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                    .param("period", "weekly")
                    .with(user(userDetails)));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.startDate").value(monday.toString()))
                    .andExpect(jsonPath("$.data.endDate").value(sunday.toString()))
                    .andExpect(jsonPath("$.data.activities").isArray())
                    .andExpect(jsonPath("$.data.activities.length()").value(2))
                    .andExpect(jsonPath("$.data.activities[0].date").value("2025-01-29"))
                    .andExpect(jsonPath("$.data.activities[0].level").value(2))
                    .andExpect(jsonPath("$.data.activities[0].wordStudy").value(true))
                    .andExpect(jsonPath("$.data.activities[0].sentenceQuiz").value(true))
                    .andDo(print());
        }

        @Test
        @WithMockUser
        @DisplayName("성공 - 월간 기록 조회")
        void getDailyRecords_Monthly_Success() throws Exception {
            // Given
            LocalDate firstDay = testDate.withDayOfMonth(1);
            LocalDate lastDay = LocalDate.of(2025, 2, 28);

            DailyRecordsResponse mockResponse = DailyRecordsResponse.builder()
                    .startDate(firstDay.toString())
                    .endDate(lastDay.toString())
                    .activities(Arrays.asList(
                            ActivityResponse.builder()
                                    .date("2025-02-01")
                                    .level(1)
                                    .wordStudy(false)
                                    .sentenceQuiz(true)
                                    .build()
                    ))
                    .build();

            given(dashboardService.getDailyRecords(eq(1L), eq("monthly"), any(LocalDate.class)))
                    .willReturn(mockResponse);

            // When
            ResultActions result = mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                    .param("period", "monthly")
                    .with(user(userDetails)));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.startDate").value(firstDay.toString()))
                    .andExpect(jsonPath("$.data.endDate").value(lastDay.toString()))
                    .andExpect(jsonPath("$.data.activities.length()").value(1))
                    .andDo(print());
        }

        @Test
        @WithMockUser
        @DisplayName("성공 - referenceDate 파라미터 사용")
        void getDailyRecords_WithReferenceDate_Success() throws Exception {
            // Given
            String referenceDate = "2025-01-15";
            LocalDate refDate = LocalDate.parse(referenceDate);
            LocalDate monday = refDate.with(DayOfWeek.MONDAY);
            LocalDate sunday = refDate.with(DayOfWeek.SUNDAY);

            DailyRecordsResponse mockResponse = DailyRecordsResponse.builder()
                    .startDate(monday.toString())
                    .endDate(sunday.toString())
                    .activities(Arrays.asList())
                    .build();

            given(dashboardService.getDailyRecords(eq(1L), eq("weekly"), eq(refDate)))
                    .willReturn(mockResponse);

            // When
            ResultActions result = mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                    .param("period", "weekly")
                    .param("referenceDate", referenceDate)
                    .with(user(userDetails)));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.startDate").value(monday.toString()))
                    .andExpect(jsonPath("$.data.endDate").value(sunday.toString()))
                    .andDo(print());
        }

        @Test
        @WithMockUser
        @DisplayName("성공 - 학습 기록 없을 때 빈 배열 반환")
        void getDailyRecords_EmptyActivities_Success() throws Exception {
            // Given
            DailyRecordsResponse mockResponse = DailyRecordsResponse.builder()
                    .startDate("2025-01-27")
                    .endDate("2025-02-02")
                    .activities(Arrays.asList())
                    .build();

            given(dashboardService.getDailyRecords(eq(1L), eq("weekly"), any(LocalDate.class)))
                    .willReturn(mockResponse);

            // When
            ResultActions result = mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                    .param("period", "weekly")
                    .with(user(userDetails)));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.activities").isArray())
                    .andExpect(jsonPath("$.data.activities.length()").value(0))
                    .andDo(print());
        }

        @Test
        @WithMockUser
        @DisplayName("실패 - period 파라미터 누락")
        void getDailyRecords_MissingPeriod_BadRequest() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                    .with(user(userDetails)));

            // Then
            result.andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @WithMockUser
        @DisplayName("실패 - 유효하지 않은 period")
        void getDailyRecords_InvalidPeriod_BadRequest() throws Exception {
            // Given
            given(dashboardService.getDailyRecords(eq(1L), eq("yearly"), any(LocalDate.class)))
                    .willThrow(new BusinessException(ErrorCode.INVALID_PERIOD));

            // When
            ResultActions result = mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                    .param("period", "yearly")
                    .with(user(userDetails)));

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andDo(print());
        }

        @Test
        @WithMockUser
        @DisplayName("실패 - 존재하지 않는 회원")
        void getDailyRecords_MemberNotFound_NotFound() throws Exception {
            // Given
            given(dashboardService.getDailyRecords(eq(1L), eq("weekly"), any(LocalDate.class)))
                    .willThrow(new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

            // When
            ResultActions result = mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                    .param("period", "weekly")
                    .with(user(userDetails)));

            // Then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패 - 인증되지 않은 사용자")
        void getDailyRecords_Unauthorized() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                    .param("period", "weekly"));

            // Then
            result.andExpect(status().isUnauthorized())
                    .andDo(print());
        }

        @Test
        @WithMockUser
        @DisplayName("실패 - 잘못된 날짜 형식")
        void getDailyRecords_InvalidDateFormat_BadRequest() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                    .param("period", "weekly")
                    .param("referenceDate", "invalid-date")
                    .with(user(userDetails)));

            // Then
            result.andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }
}
