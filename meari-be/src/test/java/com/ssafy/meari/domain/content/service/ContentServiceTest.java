package com.ssafy.meari.domain.content.service;

import com.ssafy.meari.domain.content.dto.response.ContentListResponse;
import com.ssafy.meari.domain.content.dto.response.QuizResponseDto;
import com.ssafy.meari.domain.content.dto.response.QuizWordDto;
import com.ssafy.meari.domain.content.dto.response.RoleListResponse;
import com.ssafy.meari.domain.content.dto.response.ThemeListResponse;
import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.error.exception.BusinessException;
import com.ssafy.meari.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ContentService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentService 단위 테스트")
class ContentServiceTest {

    @InjectMocks
    private ContentService contentService;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SentenceRepository sentenceRepository;

    @Test
    @DisplayName("테마 목록 조회 - 성공")
    void getThemes_Success() {
        // Given
        Theme theme1 = Theme.builder()
                .themeId(1L)
                .name("식당/카페")
                .description("식당이나 카페에서 사용하는 기본 회화")
                .themeUrl("https://cdn.../theme/cafe.jpg")
                .build();

        Theme theme2 = Theme.builder()
                .themeId(2L)
                .name("비즈니스 미팅")
                .description("비즈니스 미팅 회화")
                .themeUrl("https://cdn.../theme/business.jpg")
                .build();

        given(themeRepository.findAll()).willReturn(Arrays.asList(theme1, theme2));

        // When
        List<ThemeListResponse> result = contentService.getThemes();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getThemeId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("식당/카페");
        assertThat(result.get(1).getThemeId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("비즈니스 미팅");

        verify(themeRepository).findAll();
    }

    @Test
    @DisplayName("테마 목록 조회 - 빈 목록")
    void getThemes_Empty() {
        // Given
        given(themeRepository.findAll()).willReturn(List.of());

        // When
        List<ThemeListResponse> result = contentService.getThemes();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("테마별 콘텐츠 목록 조회 - 성공")
    void getContentsByTheme_Success() {
        // Given
        Long themeId = 1L;
        Theme theme = Theme.builder()
                .themeId(themeId)
                .name("식당/카페")
                .build();

        Content content1 = Content.builder()
                .contentId(101L)
                .theme(theme)
                .title("카페에서 아메리카노 주문하기")
                .thumbnailUrl("https://cdn.../thumb/cafe1.jpg")
                .maxPeople(2)
                .totalDuration(BigDecimal.valueOf(180))
                .build();

        Content content2 = Content.builder()
                .contentId(102L)
                .theme(theme)
                .title("카페에서 아메리카노 주문하기2")
                .thumbnailUrl("https://cdn.../thumb/cafe2.jpg")
                .maxPeople(3)
                .totalDuration(BigDecimal.valueOf(200))
                .build();

        given(themeRepository.existsById(themeId)).willReturn(true);
        given(contentRepository.findByTheme_ThemeId(themeId))
                .willReturn(Arrays.asList(content1, content2));

        // When
        List<ContentListResponse> result = contentService.getContentsByTheme(themeId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContentId()).isEqualTo(101L);
        assertThat(result.get(0).getTitle()).isEqualTo("카페에서 아메리카노 주문하기");
        assertThat(result.get(0).getMaxPeople()).isEqualTo(2);
        assertThat(result.get(1).getContentId()).isEqualTo(102L);

        verify(themeRepository).existsById(themeId);
        verify(contentRepository).findByTheme_ThemeId(themeId);
    }

    @Test
    @DisplayName("테마별 콘텐츠 목록 조회 - 존재하지 않는 테마")
    void getContentsByTheme_ThemeNotFound() {
        // Given
        Long themeId = 999L;
        given(themeRepository.existsById(themeId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> contentService.getContentsByTheme(themeId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_THEME);

        verify(themeRepository).existsById(themeId);
    }

    @Test
    @DisplayName("콘텐츠별 역할 목록 조회 - 성공")
    void getRolesByContent_Success() {
        // Given
        Long contentId = 101L;
        Theme theme = Theme.builder()
                .themeId(1L)
                .name("식당/카페")
                .build();

        Content content = Content.builder()
                .contentId(contentId)
                .theme(theme)
                .title("카페에서 아메리카노 주문하기")
                .maxPeople(2)
                .totalDuration(BigDecimal.valueOf(180))
                .build();

        Role role1 = Role.builder()
                .roleId(1L)
                .content(content)
                .name("점원")
                .build();

        Role role2 = Role.builder()
                .roleId(2L)
                .content(content)
                .name("손님")
                .build();

        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(roleRepository.findByContent_ContentId(contentId))
                .willReturn(Arrays.asList(role1, role2));

        // When
        List<RoleListResponse> result = contentService.getRolesByContent(contentId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRoleId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("점원");
        assertThat(result.get(0).getContentId()).isEqualTo(contentId);
        assertThat(result.get(1).getRoleId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("손님");

        verify(contentRepository).findById(contentId);
        verify(roleRepository).findByContent_ContentId(contentId);
    }

    @Test
    @DisplayName("콘텐츠별 역할 목록 조회 - 존재하지 않는 콘텐츠")
    void getRolesByContent_ContentNotFound() {
        // Given
        Long contentId = 999L;
        given(contentRepository.findById(contentId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> contentService.getRolesByContent(contentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_CONTENT);

        verify(contentRepository).findById(contentId);
    }

    @Test
    @DisplayName("퀴즈 조회 - 성공")
    void getQuiz_Success() {
        // Given
        Sentence sentence1 = Sentence.builder()
                .sentenceId(501L)
                .textKo("어서오세요 주문 도와드릴까요")
                .build();

        Sentence sentence2 = Sentence.builder()
                .sentenceId(502L)
                .textKo("따뜻한 아메리카노 한잔 주세요")
                .build();

        given(sentenceRepository.findRandomSentences(2))
                .willReturn(Arrays.asList(sentence1, sentence2));

        // When
        List<QuizResponseDto> result = contentService.getQuiz(2);

        // Then
        assertThat(result).hasSize(2);

        // 첫 번째 퀴즈 검증
        QuizResponseDto quiz1 = result.get(0);
        assertThat(quiz1.getSentenceId()).isEqualTo(501L);
        assertThat(quiz1.getWords()).hasSize(3);

        // 셔플되어도 모든 단어와 인덱스가 포함되어 있는지 검증
        List<String> texts1 = quiz1.getWords().stream()
                .map(QuizWordDto::getText)
                .collect(Collectors.toList());
        assertThat(texts1).containsExactlyInAnyOrder("어서오세요", "주문", "도와드릴까요");

        List<Integer> indices1 = quiz1.getWords().stream()
                .map(QuizWordDto::getIndex)
                .collect(Collectors.toList());
        assertThat(indices1).containsExactlyInAnyOrder(0, 1, 2);

        // 두 번째 퀴즈 검증
        QuizResponseDto quiz2 = result.get(1);
        assertThat(quiz2.getSentenceId()).isEqualTo(502L);
        assertThat(quiz2.getWords()).hasSize(4);

        verify(sentenceRepository).findRandomSentences(2);
    }

    @Test
    @DisplayName("퀴즈 조회 - 빈 결과")
    void getQuiz_Empty() {
        // Given
        given(sentenceRepository.findRandomSentences(5))
                .willReturn(List.of());

        // When
        List<QuizResponseDto> result = contentService.getQuiz(5);

        // Then
        assertThat(result).isEmpty();

        verify(sentenceRepository).findRandomSentences(5);
    }
}
