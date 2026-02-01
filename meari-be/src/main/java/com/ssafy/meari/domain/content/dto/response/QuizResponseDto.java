package com.ssafy.meari.domain.content.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.content.entity.Sentence;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "문장 순서 맞추기 퀴즈")
public class QuizResponseDto {

    @Schema(description = "문장 ID", example = "501")
    private Long sentenceId;

    @Schema(description = "셔플된 단어 목록 (각 단어에 원래 인덱스 포함)")
    private List<QuizWordDto> words;

    public static QuizResponseDto from(Sentence sentence) {
        String[] splitWords = sentence.getTextKo().split(" ");

        List<QuizWordDto> wordList = new ArrayList<>();
        for (int i = 0; i < splitWords.length; i++) {
            wordList.add(QuizWordDto.builder()
                    .text(splitWords[i])
                    .index(i)
                    .build());
        }

        Collections.shuffle(wordList);

        return QuizResponseDto.builder()
                .sentenceId(sentence.getSentenceId())
                .words(wordList)
                .build();
    }
}
