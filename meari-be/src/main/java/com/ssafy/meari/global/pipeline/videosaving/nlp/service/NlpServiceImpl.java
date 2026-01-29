package com.ssafy.meari.global.pipeline.videosaving.nlp.service;

import com.ssafy.meari.global.pipeline.videosaving.nlp.dto.MorphemeAnalysisResponseDto;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NlpServiceImpl implements NlpService {

    private static final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

    @Override
    public MorphemeAnalysisResponseDto analyzeMorphemes(String text) {
        log.info("[NLP] 형태소 분석 요청 - text: {}", text);

        if (text == null || text.isEmpty()) {
            log.warn("[NLP] 빈 텍스트로 형태소 분석 요청됨");
            return MorphemeAnalysisResponseDto.builder()
                    .originalText(text)
                    .morphemes(List.of())
                    .build();
        }

        try {
            log.debug("[NLP] KOMORAN.analyze() 호출 시작");
            KomoranResult komoranResult = komoran.analyze(text);
            List<Token> tokenList = komoranResult.getTokenList();
            log.debug("[NLP] analyze 결과: {} 개의 토큰", tokenList.size());

            List<MorphemeAnalysisResponseDto.Morpheme> morphemes = tokenList.stream()
                    .filter(token -> isTargetPos(token.getPos().toString()))
                    .map(token -> {
                        String lemma = toLemma(token.getMorph(), token.getPos().toString());
                        log.debug("[NLP] 토큰 처리: morph={}, lemma={}, pos={}", token.getMorph(), lemma, token.getPos());
                        return MorphemeAnalysisResponseDto.Morpheme.builder()
                                .text(lemma)
                                .pos(token.getPos().toString())
                                .confidence(1.0)
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("[NLP] 형태소 분석 완료: {} 개의 형태소 발견", morphemes.size());
            log.debug("[NLP] 형태소 목록: {}", morphemes);

            return MorphemeAnalysisResponseDto.builder()
                    .originalText(text)
                    .morphemes(morphemes)
                    .build();

        } catch (Exception e) {
            log.error("[NLP] 형태소 분석 중 오류 발생 - text: {}", text, e);
            throw new RuntimeException("형태소 분석 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MorphemeAnalysisResponseDto> analyzeMorphemesBatch(List<String> texts) {
        log.debug("배치 형태소 분석 시작: {} 개의 문장", texts.size());

        return texts.stream()
                .map(this::analyzeMorphemes)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> extractNouns(String text) {
        log.info("[NLP] 명사 추출 요청 - text: {}", text);

        if (text == null || text.isEmpty()) {
            log.warn("[NLP] 빈 텍스트로 명사 추출 요청됨");
            return List.of();
        }

        try {
            log.debug("[NLP] KOMORAN.analyze() 호출");
            KomoranResult komoranResult = komoran.analyze(text);
            List<Token> tokenList = komoranResult.getTokenList();

            List<String> nouns = tokenList.stream()
                    .filter(token -> {
                        String pos = token.getPos().toString();
                        boolean isNoun = pos.startsWith("N"); // NNG, NNP, NNB 등
                        log.debug("[NLP] 토큰 필터링: morph={}, pos={}, isNoun={}",
                                token.getMorph(), pos, isNoun);
                        return isNoun;
                    })
                    .map(token -> {
                        String lemma = toLemma(token.getMorph(), token.getPos().toString());
                        log.debug("[NLP] 사전형 변환: morph={}, lemma={}", token.getMorph(), lemma);
                        return lemma;
                    })
                    .collect(Collectors.toList());

            log.info("[NLP] 명사 추출 완료: {} 개의 명사 발견", nouns.size());
            log.debug("[NLP] 추출된 명사: {}", nouns);
            return nouns;

        } catch (Exception e) {
            log.error("[NLP] 명사 추출 중 오류 발생 - text: {}", text, e);
            throw new RuntimeException("명사 추출 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> extractByPos(String text, String pos) {
        log.info("[NLP] {}(으)로 단어 추출 요청 - text: {}", pos, text);

        if (text == null || text.isEmpty()) {
            log.warn("[NLP] 빈 텍스트로 단어 추출 요청됨");
            return List.of();
        }

        try {
            log.debug("[NLP] KOMORAN.analyze() 호출 - pos: {}", pos);
            KomoranResult komoranResult = komoran.analyze(text);
            List<Token> tokenList = komoranResult.getTokenList();

            List<String> words = tokenList.stream()
                    .filter(token -> {
                        String tokenPos = token.getPos().toString();
                        boolean matches = tokenPos.startsWith(pos);
                        log.debug("[NLP] 토큰 필터링: morph={}, pos={}, target_pos={}, matches={}",
                                token.getMorph(), tokenPos, pos, matches);
                        return matches;
                    })
                    .map(token -> {
                        String lemma = toLemma(token.getMorph(), token.getPos().toString());
                        log.debug("[NLP] 사전형 변환: morph={}, pos={}, lemma={}",
                                token.getMorph(), pos, lemma);
                        return lemma;
                    })
                    .collect(Collectors.toList());

            log.info("[NLP] {}(으)로 단어 추출 완료: {} 개의 단어 발견", pos, words.size());
            log.debug("[NLP] 추출된 단어: {}", words);
            return words;

        } catch (Exception e) {
            log.error("[NLP] {}(으)로 단어 추출 중 오류 발생 - text: {}", pos, text, e);
            throw new RuntimeException(pos + "(으)로 단어 추출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 어간을 사전형(원형)으로 변환
     * 예: '예쁘' + VA → '예쁘다', '먹' + VV → '먹다'
     *
     * @param morph 어간 (기본형)
     * @param pos 품사 태그
     * @return 사전형 (원형)
     */
    private String toLemma(String morph, String pos) {
        // 동사(VV), 형용사(VA), 보조용언(VX), 긍정지정사(VCP), 부정지정사(VCN)
        if (pos.startsWith("VV") || pos.startsWith("VA") || pos.startsWith("VX") ||
            pos.startsWith("VCP") || pos.startsWith("VCN")) {
            return morph + "다";
        }
        // 그 외는 그대로 반환
        return morph;
    }

    /**
     * 분석 대상 품사인지 확인
     * 관형사, 명사, 대명사, 동사, 형용사만 추출
     *
     * @param pos 품사 태그
     * @return 대상 품사 여부
     */
    private boolean isTargetPos(String pos) {
        // MM: 관형사
        // NNG, NNP, NNB: 명사 (일반명사, 고유명사, 의존명사)
        // NP: 대명사
        // VV: 동사
        // VA: 형용사
        return pos.startsWith("MM") ||      // 관형사
               pos.startsWith("NN") ||      // 명사 (NNG, NNP, NNB)
               pos.startsWith("NP") ||      // 대명사
               pos.startsWith("VV") ||      // 동사
               pos.startsWith("VA");        // 형용사
    }
}
