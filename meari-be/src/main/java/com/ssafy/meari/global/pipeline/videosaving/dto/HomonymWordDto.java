package com.ssafy.meari.global.pipeline.videosaving.dto;

import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.word.entity.Word;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 동음이의어 정보를 담는 DTO
 * 나중에 LLM으로 뜻풀이(definitionKr) 기반 판별 시 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomonymWordDto {

	private Sentence sentence;
	private Integer sentenceSequence;  // 문장 시퀀스 번호 (대본에서 몇 번째 문장인지)
	private String wordKr;
	private List<Word> homonymWords;   // 동음이의어 목록 (definitionKr로 구분)
}
