package com.ssafy.meari.global.pipeline.videosaving.nlp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.global.pipeline.videosaving.nlp.service.NlpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NlpController.class)
class NlpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NlpService nlpService;

    @DisplayName("형태소 분석 요청 - 성공")
    @Test
    void analyzeMorphemes_success() throws Exception {
        // Given
        String text = "안녕하세요";

        // When & Then
        mockMvc.perform(post("/api/v1/nlp/morpheme-analysis")
                .param("text", text)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @DisplayName("형태소 분석 요청 - 빈 텍스트")
    @Test
    void analyzeMorphemes_emptyText() throws Exception {
        // Given
        String text = "";

        // When & Then
        mockMvc.perform(post("/api/v1/nlp/morpheme-analysis")
                .param("text", text)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @DisplayName("배치 형태소 분석 요청 - 성공")
    @Test
    void analyzeMorphemesBatch_success() throws Exception {
        // Given
        List<String> texts = List.of(
                "안녕하세요",
                "반갑습니다",
                "잘 지내세요"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/nlp/morpheme-analysis/batch")
                .content(objectMapper.writeValueAsString(texts))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @DisplayName("명사 추출 요청 - 성공")
    @Test
    void extractNouns_success() throws Exception {
        // Given
        String text = "서울에서 친구를 만났습니다";

        // When & Then
        mockMvc.perform(post("/api/v1/nlp/extract-nouns")
                .param("text", text)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @DisplayName("품사별 단어 추출 요청 - 성공")
    @Test
    void extractByPos_success() throws Exception {
        // Given
        String text = "먹고 마시고 자다";
        String pos = "Verb";

        // When & Then
        mockMvc.perform(post("/api/v1/nlp/extract-by-pos")
                .param("text", text)
                .param("pos", pos)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @DisplayName("형태소 분석 요청 - 파라미터 누락")
    @Test
    void analyzeMorphemes_missingParameter() throws Exception {
        // Given
        // text 파라미터 없음

        // When & Then
        mockMvc.perform(post("/api/v1/nlp/morpheme-analysis")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("배치 형태소 분석 - 빈 리스트")
    @Test
    void analyzeMorphemesBatch_emptyList() throws Exception {
        // Given
        List<String> texts = List.of();

        // When & Then
        mockMvc.perform(post("/api/v1/nlp/morpheme-analysis/batch")
                .content(objectMapper.writeValueAsString(texts))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
