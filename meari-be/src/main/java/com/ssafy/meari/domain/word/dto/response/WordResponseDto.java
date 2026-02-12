package com.ssafy.meari.domain.word.dto.response;

import com.ssafy.meari.domain.word.entity.Word;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WordResponseDto {
    private Long wordId;
    private String wordKr;
    private String definitionKr;
    private String wordVn;
    private String definitionVn;

    public static WordResponseDto from(Word word) {
        return WordResponseDto.builder()
                .wordId(word.getWordId())
                .wordKr(word.getWordKr())
                .definitionKr(word.getDefinitionKr())
                .wordVn(word.getWordVn())
                .definitionVn(word.getDefinitionVn())
                .build();
    }
}
