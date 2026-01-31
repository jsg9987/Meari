package com.ssafy.meari.global.pipeline.videosaving.dto;

import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.word.entity.Word;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 단어 매칭 정보를 담는 DTO
 * SINGLE/HOMONYM 모두 저장 후 한꺼번에 SentenceWord로 변환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordMatchingInfo {

	private Sentence sentence;
	private Word word;
	private Integer originalSequence;  // 원래 순서 (정렬용)
}
