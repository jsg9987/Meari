package com.ssafy.meari.domain.solo_practice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.domain.solo_practice.dto.request.SoloPracticeStartRequest;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeContentResponse;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeRoleResponse;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeSentenceResponse;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeStartResponse;
import com.ssafy.meari.domain.solo_practice.service.SoloPracticeService;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("SoloPracticeController 테스트")
@Disabled("JwtAuthenticationFilter가 MockMvc 요청 가로챔 — Security 우회 적용 후 재활성화")
class SoloPracticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SoloPracticeService soloPracticeService;

    private SoloPracticeStartResponse mockResponse;

    @BeforeEach
    void setUp() {
        SoloPracticeContentResponse contentResponse = SoloPracticeContentResponse.builder()
            .contentId(1L)
            .title("Restaurant Ordering")
            .videoUrl("https://example.com/video.mp4")
            .thumbnailUrl("https://example.com/thumb.jpg")
            .totalDuration(new BigDecimal("125.5"))
            .build();

        SoloPracticeRoleResponse selectedRoleResponse = SoloPracticeRoleResponse.builder()
            .roleId(1L)
            .name("Customer")
            .build();

        List<SoloPracticeRoleResponse> allRolesResponse = List.of(
            SoloPracticeRoleResponse.builder().roleId(1L).name("Customer").build(),
            SoloPracticeRoleResponse.builder().roleId(2L).name("Waiter").build()
        );

        List<SoloPracticeSentenceResponse> sentencesResponse = List.of(
            SoloPracticeSentenceResponse.builder()
                .sentenceId(1L)
                .sequence(0)
                .startTime(0.0)
                .endTime(2.5)
                .textKo("안녕하세요")
                .textVn("Xin chào")
                .roleId(1L)
                .roleName("Customer")
                .build(),
            SoloPracticeSentenceResponse.builder()
                .sentenceId(2L)
                .sequence(1)
                .startTime(2.5)
                .endTime(4.0)
                .textKo("어서오세요")
                .textVn("Chào mừng")
                .roleId(2L)
                .roleName("Waiter")
                .build()
        );

        mockResponse = SoloPracticeStartResponse.builder()
            .content(contentResponse)
            .selectedRole(selectedRoleResponse)
            .allRoles(allRolesResponse)
            .sentences(sentencesResponse)
            .build();
    }

    @Test
    @DisplayName("혼자연습 시작 - 성공")
    void testStartPractice_Success() throws Exception {
        // Given
        SoloPracticeStartRequest request = SoloPracticeStartRequest.builder()
            .contentId(1L)
            .roleId(1L)
            .build();

        when(soloPracticeService.startPractice(1L, 1L)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/solo-practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.contentId").value(1L))
            .andExpect(jsonPath("$.data.content.title").value("Restaurant Ordering"))
            .andExpect(jsonPath("$.data.selectedRole.roleId").value(1L))
            .andExpect(jsonPath("$.data.selectedRole.name").value("Customer"))
            .andExpect(jsonPath("$.data.allRoles").isArray())
            .andExpect(jsonPath("$.data.allRoles.length()").value(2))
            .andExpect(jsonPath("$.data.sentences").isArray())
            .andExpect(jsonPath("$.data.sentences.length()").value(2))
            .andExpect(jsonPath("$.data.sentences[0].sequence").value(0))
            .andExpect(jsonPath("$.data.sentences[0].textKo").value("안녕하세요"))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("혼자연습 시작 - Content 없음")
    void testStartPractice_ContentNotFound() throws Exception {
        // Given
        SoloPracticeStartRequest request = SoloPracticeStartRequest.builder()
            .contentId(999L)
            .roleId(1L)
            .build();

        when(soloPracticeService.startPractice(999L, 1L))
            .thenThrow(new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        // When & Then
        mockMvc.perform(post("/api/v1/solo-practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("혼자연습 시작 - Role 없음")
    void testStartPractice_RoleNotFound() throws Exception {
        // Given
        SoloPracticeStartRequest request = SoloPracticeStartRequest.builder()
            .contentId(1L)
            .roleId(999L)
            .build();

        when(soloPracticeService.startPractice(1L, 999L))
            .thenThrow(new BusinessException(ErrorCode.NOT_FOUND_ROLE));

        // When & Then
        mockMvc.perform(post("/api/v1/solo-practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("혼자연습 시작 - Role이 Content에 속하지 않음")
    void testStartPractice_InvalidRoleForContent() throws Exception {
        // Given
        SoloPracticeStartRequest request = SoloPracticeStartRequest.builder()
            .contentId(1L)
            .roleId(1L)
            .build();

        when(soloPracticeService.startPractice(1L, 1L))
            .thenThrow(new BusinessException(ErrorCode.INVALID_ROLE_FOR_CONTENT));

        // When & Then
        mockMvc.perform(post("/api/v1/solo-practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("혼자연습 시작 - 요청 검증 실패 (contentId 없음)")
    void testStartPractice_MissingContentId() throws Exception {
        // Given
        String invalidRequest = "{\"roleId\": 1}";

        // When & Then
        mockMvc.perform(post("/api/v1/solo-practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("혼자연습 시작 - 요청 검증 실패 (roleId 없음)")
    void testStartPractice_MissingRoleId() throws Exception {
        // Given
        String invalidRequest = "{\"contentId\": 1}";

        // When & Then
        mockMvc.perform(post("/api/v1/solo-practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }
}
