package com.ssafy.meari.domain.solo_practice.service;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeStartResponse;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SoloPracticeService 테스트")
class SoloPracticeServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SentenceRepository sentenceRepository;

    @InjectMocks
    private SoloPracticeService soloPracticeService;

    private Content content;
    private Role role1;
    private Role role2;
    private List<Sentence> sentences;

    @BeforeEach
    void setUp() {
        // Theme 생성
        Theme theme = Theme.builder()
                .themeId(1L)
                .name("Business")
                .build();

        // Content 생성
        content = Content.builder()
                .contentId(1L)
                .title("Restaurant Ordering")
                .videoUrl("https://example.com/video.mp4")
                .thumbnailUrl("https://example.com/thumb.jpg")
                .totalDuration(new BigDecimal("125.5"))
                .maxPeople(4)
                .theme(theme)
                .build();

        // Role 생성
        role1 = Role.builder()
                .roleId(1L)
                .name("Customer")
                .content(content)
                .build();

        role2 = Role.builder()
                .roleId(2L)
                .name("Waiter")
                .content(content)
                .build();

        // Sentence 생성
        Sentence sentence1 = Sentence.builder()
                .sentenceId(1L)
                .sequence(0)
                .startTime(new BigDecimal("0.0"))
                .endTime(new BigDecimal("2.5"))
                .textKo("안녕하세요")
                .textVn("Xin chào")
                .role(role1)
                .content(content)
                .build();

        Sentence sentence2 = Sentence.builder()
                .sentenceId(2L)
                .sequence(1)
                .startTime(new BigDecimal("2.5"))
                .endTime(new BigDecimal("4.0"))
                .textKo("어서오세요")
                .textVn("Chào mừng")
                .role(role2)
                .content(content)
                .build();

        sentences = List.of(sentence1, sentence2);
    }

    @Test
    @DisplayName("혼자연습 시작 - 성공")
    void testStartPractice_Success() {
        // Given
        Long contentId = 1L;
        Long roleId = 1L;

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role1));
        when(roleRepository.findByContent_ContentId(contentId)).thenReturn(List.of(role1, role2));
        when(sentenceRepository.findByContent_ContentId(contentId)).thenReturn(sentences);

        // When
        SoloPracticeStartResponse response = soloPracticeService.startPractice(contentId, roleId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent().getContentId()).isEqualTo(1L);
        assertThat(response.getContent().getTitle()).isEqualTo("Restaurant Ordering");
        assertThat(response.getSelectedRole().getRoleId()).isEqualTo(1L);
        assertThat(response.getSelectedRole().getName()).isEqualTo("Customer");
        assertThat(response.getAllRoles()).hasSize(2);
        assertThat(response.getSentences()).hasSize(2);
        assertThat(response.getSentences().get(0).getSequence()).isEqualTo(0);
        assertThat(response.getSentences().get(1).getSequence()).isEqualTo(1);

        verify(contentRepository).findById(contentId);
        verify(roleRepository).findById(roleId);
        verify(roleRepository).findByContent_ContentId(contentId);
        verify(sentenceRepository).findByContent_ContentId(contentId);
    }

    @Test
    @DisplayName("혼자연습 시작 - Content 없음")
    void testStartPractice_ContentNotFound() {
        // Given
        Long contentId = 999L;
        Long roleId = 1L;

        when(contentRepository.findById(contentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> soloPracticeService.startPractice(contentId, roleId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND_CONTENT);

        verify(contentRepository).findById(contentId);
        verify(roleRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("혼자연습 시작 - Role 없음")
    void testStartPractice_RoleNotFound() {
        // Given
        Long contentId = 1L;
        Long roleId = 999L;

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> soloPracticeService.startPractice(contentId, roleId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND_ROLE);

        verify(contentRepository).findById(contentId);
        verify(roleRepository).findById(roleId);
        verify(sentenceRepository, never()).findByContent_ContentId(anyLong());
    }

    @Test
    @DisplayName("혼자연습 시작 - Role이 Content에 속하지 않음")
    void testStartPractice_InvalidRoleForContent() {
        // Given
        Long contentId = 1L;
        Long roleId = 1L;

        // 다른 Content에 속한 Role 생성
        Content otherContent = Content.builder()
                .contentId(2L)
                .title("Other")
                .videoUrl("https://example.com/other.mp4")
                .thumbnailUrl("https://example.com/other-thumb.jpg")
                .totalDuration(new BigDecimal("100.0"))
                .maxPeople(4)
                .theme(Theme.builder().themeId(1L).name("Business").build())
                .build();

        Role roleFromOtherContent = Role.builder()
                .roleId(1L)
                .name("Customer")
                .content(otherContent)
                .build();

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(roleFromOtherContent));

        // When & Then
        assertThatThrownBy(() -> soloPracticeService.startPractice(contentId, roleId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROLE_FOR_CONTENT);

        verify(contentRepository).findById(contentId);
        verify(roleRepository).findById(roleId);
        verify(sentenceRepository, never()).findByContent_ContentId(anyLong());
    }

    @Test
    @DisplayName("혼자연습 시작 - Sentence 정렬 확인")
    void testStartPractice_SentenceSorting() {
        // Given
        Long contentId = 1L;
        Long roleId = 1L;

        // Sentence를 역순으로 저장 (정렬 테스트용)
        List<Sentence> unsortedSentences = List.of(sentences.get(1), sentences.get(0));

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role1));
        when(roleRepository.findByContent_ContentId(contentId)).thenReturn(List.of(role1, role2));
        when(sentenceRepository.findByContent_ContentId(contentId)).thenReturn(unsortedSentences);

        // When
        SoloPracticeStartResponse response = soloPracticeService.startPractice(contentId, roleId);

        // Then - sequence 순서대로 정렬되어야 함
        assertThat(response.getSentences())
                .extracting("sequence")
                .containsExactly(0, 1);
    }
}
