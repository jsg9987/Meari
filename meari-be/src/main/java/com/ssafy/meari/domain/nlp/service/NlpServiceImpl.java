package com.ssafy.meari.domain.nlp.service;

import com.ssafy.meari.domain.nlp.dto.MorphemeAnalysisResponseDto;
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
                    .map(token -> {
                        log.debug("[NLP] 토큰 처리: morph={}, pos={}", token.getMorph(), token.getPos());
                        return MorphemeAnalysisResponseDto.Morpheme.builder()
                                .text(token.getMorph())
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
                    .map(Token::getMorph)
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
                    .map(Token::getMorph)
                    .collect(Collectors.toList());

            log.info("[NLP] {}(으)로 단어 추출 완료: {} 개의 단어 발견", pos, words.size());
            log.debug("[NLP] 추출된 단어: {}", words);
            return words;

        } catch (Exception e) {
            log.error("[NLP] {}(으)로 단어 추출 중 오류 발생 - text: {}", pos, text, e);
            throw new RuntimeException(pos + "(으)로 단어 추출 실패: " + e.getMessage(), e);
        }
    }
}
