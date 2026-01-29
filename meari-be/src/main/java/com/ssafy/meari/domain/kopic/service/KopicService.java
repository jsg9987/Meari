package com.ssafy.meari.domain.kopic.service;

import com.ssafy.meari.domain.kopic.dto.response.KopicSentenceResponse;
import com.ssafy.meari.domain.kopic.entity.KopicSentence;
import com.ssafy.meari.domain.kopic.repository.KopicSentenceRepository;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KopicService {

    private static final int RANDOM_SENTENCE_COUNT = 5;

    private final KopicSentenceRepository kopicSentenceRepository;
    private final ThemeRepository themeRepository;

    public List<KopicSentenceResponse> getRandomKopicSentences(Long themeId) {
        themeRepository.findById(themeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_THEME));

        List<KopicSentence> sentences = kopicSentenceRepository.findByTheme_ThemeId(themeId);

        if (sentences.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_KOPIC_SENTENCE);
        }

        List<KopicSentence> shuffled = new ArrayList<>(sentences);
        Collections.shuffle(shuffled);

        return shuffled.stream()
                .limit(RANDOM_SENTENCE_COUNT)
                .map(KopicSentenceResponse::from)
                .toList();
    }
}
