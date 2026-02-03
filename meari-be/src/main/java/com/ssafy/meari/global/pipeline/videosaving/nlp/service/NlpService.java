package com.ssafy.meari.global.pipeline.videosaving.nlp.service;

import com.ssafy.meari.global.pipeline.videosaving.nlp.dto.MorphemeAnalysisResponseDto;

import java.util.List;


public interface NlpService {

    /**
     * OKT를 사용하여 문장의 형태소를 분석합니다.
     *
     * @param text 분석할 문장
     * @return 형태소 분석 결과
     */
    MorphemeAnalysisResponseDto analyzeMorphemes(String text);

    /**
     * 문장 목록에 대해 형태소 분석을 수행합니다.
     *
     * @param texts 분석할 문장 목록
     * @return 형태소 분석 결과 목록
     */
    List<MorphemeAnalysisResponseDto> analyzeMorphemesBatch(List<String> texts);

    /**
     * 문장에서 명사만 추출합니다.
     *
     * @param text 분석할 문장
     * @return 명사 목록
     */
    List<String> extractNouns(String text);

    /**
     * 문장에서 특정 품사의 단어를 추출합니다.
     *
     * @param text 분석할 문장
     * @param pos 품사 (예: "Noun", "Verb", "Adjective")
     * @return 해당 품사의 단어 목록
     */
    List<String> extractByPos(String text, String pos);
}
