package com.ssafy.meari.domain.word.service;

import com.ssafy.meari.domain.word.dto.response.WordResponseDto;
import com.ssafy.meari.domain.word.entity.Word;
import com.ssafy.meari.domain.word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository wordRepository;

    public List<WordResponseDto> getRandomWords() {
        List<Word> randomWords = wordRepository.findRandomWordsLimit10();
        return randomWords.stream()
                .map(WordResponseDto::from)
                .collect(Collectors.toList());
    }
}
