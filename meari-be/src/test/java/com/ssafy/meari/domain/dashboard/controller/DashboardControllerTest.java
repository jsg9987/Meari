package com.ssafy.meari.domain.dashboard.controller;

import com.ssafy.meari.domain.dashboard.dto.response.DailyRecordsResponse;
import com.ssafy.meari.domain.dashboard.service.DashboardService;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@DisplayName("DashboardController 단위 테스트")
class DashboardControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DashboardService dashboardService;

        private Member testMember;
        private UserDetailsImpl userDetails;

        @BeforeEach
        void setUp() {
                testMember = Member.builder()
                                .email("test@example.com")
                                .password("password123")
                                .nickname("테스터")
                                .nativeLanguage(NativeLanguage.KR)
                                .build();

                userDetails = new UserDetailsImpl(testMember);
        }

        @Test
        @DisplayName("성공 - 일일학습 기록 조회 (주간)")
        @WithMockUser
        void getDailyRecords_Weekly_Success() throws Exception {
                // Given
                DailyRecordsResponse response = DailyRecordsResponse.builder()
                                .startDate("2025-01-27")
                                .endDate("2025-02-02")
                                .build();

                given(dashboardService.getDailyRecords(any(Long.class), eq("weekly"), any(LocalDate.class)))
                                .willReturn(response);

                // When & Then
                mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                                .param("period", "weekly")
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.startDate").value("2025-01-27"))
                                .andExpect(jsonPath("$.data.endDate").value("2025-02-02"));

                verify(dashboardService, times(1))
                                .getDailyRecords(any(Long.class), eq("weekly"), any(LocalDate.class));
        }

        @Test
        @DisplayName("성공 - 학습 완료 기록")
        @WithMockUser
        void recordLearningCompletion_Success() throws Exception {
                // Given
                doNothing().when(dashboardService)
                                .recordLearningCompletion(any(Long.class), any(LocalDate.class));

                // When & Then
                mockMvc.perform(post("/api/v1/dashboard/me/learning-completion")
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isEmpty());

                verify(dashboardService, times(1))
                                .recordLearningCompletion(any(Long.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("실패 - 학습 완료 기록 (인증되지 않은 사용자)")
        void recordLearningCompletion_Unauthorized() throws Exception {
                // When & Then
                mockMvc.perform(post("/api/v1/dashboard/me/learning-completion"))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());

                verify(dashboardService, never())
                                .recordLearningCompletion(any(Long.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("실패 - 학습 완료 기록 (존재하지 않는 회원)")
        @WithMockUser
        void recordLearningCompletion_MemberNotFound() throws Exception {
                // Given
                doThrow(new BusinessException(ErrorCode.NOT_FOUND_MEMBER))
                                .when(dashboardService)
                                .recordLearningCompletion(any(Long.class), any(LocalDate.class));

                // When & Then
                mockMvc.perform(post("/api/v1/dashboard/me/learning-completion")
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isNotFound());

                verify(dashboardService, times(1))
                                .recordLearningCompletion(any(Long.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("실패 - 일일학습 기록 조회 (잘못된 period)")
        @WithMockUser
        void getDailyRecords_InvalidPeriod() throws Exception {
                // Given
                given(dashboardService.getDailyRecords(any(Long.class), eq("invalid"), any(LocalDate.class)))
                                .willThrow(new BusinessException(ErrorCode.INVALID_PERIOD));

                // When & Then
                mockMvc.perform(get("/api/v1/dashboard/me/daily-records")
                                .param("period", "invalid")
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isBadRequest());

                verify(dashboardService, times(1))
                                .getDailyRecords(any(Long.class), eq("invalid"), any(LocalDate.class));
        }
}
