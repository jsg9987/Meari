package com.ssafy.meari.domain.content.service;

import com.ssafy.meari.domain.content.dto.response.ContentListResponse;
import com.ssafy.meari.domain.content.dto.response.QuizResponseDto;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 콘텐츠 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ThemeRepository themeRepository;
    private final ContentRepository contentRepository;
    private final RoleRepository roleRepository;
    private final SentenceRepository sentenceRepository;

    /**
     * 테마 목록 조회
     */
    public List<ThemeListResponse> getThemes() {
        log.debug("테마 목록 조회");

        List<Theme> themes = themeRepository.findAll();

        return themes.stream()
                .map(theme -> ThemeListResponse.builder()
                        .themeId(theme.getThemeId())
                        .name(theme.getName())
                        .description(theme.getDescription())
                        .themeUrl(theme.getThemeUrl())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 테마의 콘텐츠 목록 조회
     */
    public List<ContentListResponse> getContentsByTheme(Long themeId) {
        log.debug("테마별 콘텐츠 목록 조회: themeId={}", themeId);

        // 테마 존재 여부 확인
        if (!themeRepository.existsById(themeId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_THEME);
        }

        List<Content> contents = contentRepository.findByTheme_ThemeId(themeId);

        return contents.stream()
                .map(content -> ContentListResponse.builder()
                        .contentId(content.getContentId())
                        .title(content.getTitle())
                        .thumbnailUrl(content.getThumbnailUrl())
                        .maxPeople(content.getMaxPeople())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 콘텐츠의 역할(캐릭터) 목록 조회
     */
    public List<RoleListResponse> getRolesByContent(Long contentId) {
        log.debug("콘텐츠별 역할 목록 조회: contentId={}", contentId);

        // 콘텐츠 존재 여부 확인
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        List<Role> roles = roleRepository.findByContent_ContentId(contentId);

        return roles.stream()
                .map(role -> RoleListResponse.builder()
                        .roleId(role.getRoleId())
                        .contentId(content.getContentId())
                        .name(role.getName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 문장 순서 맞추기 퀴즈 조회
     */
    public List<QuizResponseDto> getQuiz(int count) {
        log.debug("퀴즈 조회: count={}", count);

        List<Sentence> sentences = sentenceRepository.findRandomSentences(count);

        return sentences.stream()
                .map(QuizResponseDto::from)
                .collect(Collectors.toList());
    }
}
