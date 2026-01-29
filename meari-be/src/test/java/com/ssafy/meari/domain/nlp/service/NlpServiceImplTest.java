package com.ssafy.meari.domain.nlp.service;

import com.ssafy.meari.domain.nlp.dto.MorphemeAnalysisResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NlpServiceImplTest {

    @Autowired
    private NlpService nlpService;

    @DisplayName("정상 문장의 형태소 분석")
    @Test
    void analyzeMorphemes_success() {
        // Given
        String text = "안녕하세요. 반갑습니다.";

        // When
        MorphemeAnalysisResponseDto result = nlpService.analyzeMorphemes(text);

        // Then
        assertNotNull(result);
        assertEquals(text, result.getOriginalText());
        assertFalse(result.getMorphemes().isEmpty());
        assertTrue(result.getMorphemes().stream()
                .anyMatch(m -> m.getText().equals("안녕") || m.getText().equals("하세요")));
    }

    @DisplayName("빈 문자열 형태소 분석")
    @Test
    void analyzeMorphemes_emptyString() {
        // Given
        String text = "";

        // When
        MorphemeAnalysisResponseDto result = nlpService.analyzeMorphemes(text);

        // Then
        assertNotNull(result);
        assertEquals(text, result.getOriginalText());
        assertTrue(result.getMorphemes().isEmpty());
    }

    @DisplayName("null 입력 형태소 분석")
    @Test
    void analyzeMorphemes_nullInput() {
        // Given
        String text = null;

        // When & Then
        assertDoesNotThrow(() -> nlpService.analyzeMorphemes(text));
    }

    @DisplayName("명사 추출")
    @Test
    void extractNouns_success() {
        // Given
        String text = "서울에서 친구를 만났습니다.";

        // When
        List<String> nouns = nlpService.extractNouns(text);

        // Then
        assertNotNull(nouns);
        assertFalse(nouns.isEmpty());
        assertTrue(nouns.contains("서울") || nouns.contains("친구"));
    }

    @DisplayName("명사 추출 - 빈 문자열")
    @Test
    void extractNouns_emptyString() {
        // Given
        String text = "";

        // When
        List<String> nouns = nlpService.extractNouns(text);

        // Then
        assertNotNull(nouns);
        assertTrue(nouns.isEmpty());
    }

    @DisplayName("명사 추출 - null 입력")
    @Test
    void extractNouns_nullInput() {
        // Given
        String text = null;

        // When
        List<String> nouns = nlpService.extractNouns(text);

        // Then
        assertNotNull(nouns);
        assertTrue(nouns.isEmpty());
    }

    @DisplayName("품사별 단어 추출 - Verb")
    @Test
    void extractByPos_verb() {
        // Given
        String text = "먹고 마시고 자다";
        String pos = "Verb";

        // When
        List<String> verbs = nlpService.extractByPos(text, pos);

        // Then
        assertNotNull(verbs);
        assertFalse(verbs.isEmpty());
    }

    @DisplayName("배치 형태소 분석")
    @Test
    void analyzeMorphemesBatch_success() {
        // Given
        List<String> texts = List.of(
                "안녕하세요",
                "반갑습니다",
                "잘 지내세요"
        );

        // When
        List<MorphemeAnalysisResponseDto> results = nlpService.analyzeMorphemesBatch(texts);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        results.forEach(result -> {
            assertNotNull(result.getOriginalText());
            assertFalse(result.getMorphemes().isEmpty());
        });
    }

    @DisplayName("배치 형태소 분석 - 빈 리스트")
    @Test
    void analyzeMorphemesBatch_emptyList() {
        // Given
        List<String> texts = List.of();

        // When
        List<MorphemeAnalysisResponseDto> results = nlpService.analyzeMorphemesBatch(texts);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @DisplayName("복잡한 문장 형태소 분석")
    @Test
    void analyzeMorphemes_complexSentence() {
        // Given
        String text = "자연어 처리는 컴퓨터가 인간의 언어를 이해하고 생성하는 기술입니다.";

        // When
        MorphemeAnalysisResponseDto result = nlpService.analyzeMorphemes(text);

        // Then
        assertNotNull(result);
        assertFalse(result.getMorphemes().isEmpty());
        assertTrue(result.getMorphemes().size() > 5);
    }

    @DisplayName("특수문자 포함 문장 형태소 분석")
    @Test
    void analyzeMorphemes_specialCharacters() {
        // Given
        String text = "Hello, 안녕하세요! How are you?";

        // When
        MorphemeAnalysisResponseDto result = nlpService.analyzeMorphemes(text);

        // Then
        assertNotNull(result);
        assertFalse(result.getMorphemes().isEmpty());
    }
}
