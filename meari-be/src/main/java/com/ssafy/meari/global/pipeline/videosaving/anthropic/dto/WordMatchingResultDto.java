package com.ssafy.meari.global.pipeline.videosaving.anthropic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 단어 매칭 결과 DTO (CSV 리포트용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordMatchingResultDto {

	private int sentenceSequence;      // 문장 순서
	private String sentenceTextKo;     // 문장 한국어 텍스트
	private String wordKr;             // 매칭된 단어 (한국어)
	private String definitionKr;       // 단어 뜻 (한국어)
	private String wordVn;             // 매칭된 단어 (베트남어)
	private String definitionVn;       // 단어 뜻 (베트남어)
	private String matchType;          // 매칭 타입: "SINGLE" 또는 "HOMONYM"
}
