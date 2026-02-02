package com.ssafy.meari.global.pipeline.videosaving.nlp.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ssafy.meari.global.pipeline.videosaving.nlp.dto.MorphemeAnalysisResponseDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NlpServiceImpl implements NlpService {

    private static final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

    // 불용어 리스트 (학습 가치가 낮은 일반적인 단어들)
    private static final Set<String> STOPWORDS = Set.of(
        // 일반 동사
        "하다", "되다", "있다", "없다", "같다", "보다", "가다", "오다",
        "주다", "받다", "알다", "모르다", "나다", "들다", "쓰다",
        // 일반 형용사
        "좋다", "나쁘다", "크다", "작다", "많다", "적다", "높다", "낮다",
        "길다", "짧다", "넓다", "좁다", "깊다", "얕다",
        // 대명사 & 소유격 관형사
        "나", "너", "우리", "것", "거", "내", "네",
        // 관형사
        "이", "그", "어떤", "무슨", "어느"
    );

    @Override
    public MorphemeAnalysisResponseDto analyzeMorphemes(String text) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[NLP] 형태소 분석 시작");
        log.info("[NLP] 원문: \"{}\"", text);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (text == null || text.isEmpty()) {
            log.warn("[NLP] 빈 텍스트로 형태소 분석 요청됨");
            return MorphemeAnalysisResponseDto.builder()
                .originalText(text)
                .morphemes(List.of())
                .build();
        }

        try {
            log.info("[NLP] KOMORAN 형태소 분석 시작...");
            KomoranResult komoranResult = komoran.analyze(text);
            List<Token> tokenList = komoranResult.getTokenList();
            log.info("[NLP] ✅ KOMORAN 분석 완료: 총 {} 개의 토큰 발견", tokenList.size());
            log.info("");

            // 전체 토큰 출력
            log.info("[NLP] 📋 전체 토큰 목록:");
            for (int i = 0; i < tokenList.size(); i++) {
                Token token = tokenList.get(i);
                log.info("[NLP]   {}. \"{}\" (품사: {})", i + 1, token.getMorph(), token.getPos());
            }
            log.info("");

            // 필터링 및 처리
            log.info("[NLP] 🔍 단어 필터링 및 변환 과정:");
            List<MorphemeAnalysisResponseDto.Morpheme> morphemes = tokenList.stream()
                .map(token -> {
                    String pos = token.getPos().toString();
                    String morph = token.getMorph();
                    String lemma = toLemma(morph, pos);
                    boolean isTarget = isTargetPos(pos);
                    boolean isStop = isStopword(lemma);

                    // 상세 로그 출력
                    if (!isTarget) {
                        log.info("[NLP]   ❌ \"{}\" (품사: {}) - 대상 품사 아님 (제외)", morph, pos);
                    } else if (isStop) {
                        log.info("[NLP]   ❌ \"{}\" → \"{}\" (품사: {}) - 불용어 (제외)", morph, lemma, pos);
                    } else {
                        log.info("[NLP]   ✅ \"{}\" → \"{}\" (품사: {}) - 저장됨", morph, lemma, pos);
                    }

                    return new Object[] { token, lemma, isTarget, isStop };
                })
                .filter(arr -> (boolean) arr[2])  // 대상 품사 필터링
                .filter(arr -> !(boolean) arr[3]) // 불용어 필터링
                .map(arr -> {
                    Token token = (Token) arr[0];
                    String lemma = (String) arr[1];
                    return MorphemeAnalysisResponseDto.Morpheme.builder()
                        .text(lemma)
                        .pos(token.getPos().toString())
                        .confidence(1.0)
                        .build();
                })
                .collect(Collectors.toList());

            log.info("");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("[NLP] 📊 최종 결과: {} 개의 단어 저장됨", morphemes.size());
            if (!morphemes.isEmpty()) {
                log.info("[NLP] 💾 저장된 단어 목록 (표):");
                log.info("[NLP] ┌────────────────────────────────┬──────────────┬──────────┬────────────────┐");
                log.info("[NLP] │ 문장                           │ 단어         │ 품사     │ 뜻             │");
                log.info("[NLP] ├────────────────────────────────┼──────────────┼──────────┼────────────────┤");
                for (MorphemeAnalysisResponseDto.Morpheme m : morphemes) {
                    String posDesc = getPosDescription(m.getPos());
                    log.info("[NLP] │ {:<30} │ {:<12} │ {:<8} │ {:<14} │",
                        truncate(text, 30),
                        truncate(m.getText(), 12),
                        m.getPos(),
                        posDesc);
                }
                log.info("[NLP] └────────────────────────────────┴──────────────┴──────────┴────────────────┘");
            }
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return MorphemeAnalysisResponseDto.builder()
                .originalText(text)
                .morphemes(morphemes)
                .build();

        } catch (Exception e) {
            log.error("[NLP] ❌ 형태소 분석 중 오류 발생 - text: {}", text, e);
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
     * 관형사, 명사(의존명사 제외), 대명사, 동사, 형용사만 추출
     *
     * @param pos 품사 태그
     * @return 대상 품사 여부
     */
    private boolean isTargetPos(String pos) {
        // MM: 관형사
        // NNG: 일반명사, NNP: 고유명사 (NNB 의존명사는 제외)
        // NP: 대명사
        // VV: 동사
        // VA: 형용사
        if (pos.equals("NNB")) {
            return false;  // 의존명사 제외 (것, 거, 수, 줄, 데 등)
        }
        return pos.startsWith("MM") ||      // 관형사
               pos.equals("NNG") ||         // 일반명사
               pos.equals("NNP") ||         // 고유명사
               pos.startsWith("NP") ||      // 대명사
               pos.startsWith("VV") ||      // 동사
               pos.startsWith("VA");        // 형용사
    }

    /**
     * 불용어인지 확인
     *
     * @param word 단어 (사전형)
     * @return 불용어 여부
     */
    private boolean isStopword(String word) {
        return STOPWORDS.contains(word);
    }

    /**
     * 품사 태그를 한글 설명으로 변환
     *
     * @param pos 품사 태그
     * @return 한글 설명
     */
    private String getPosDescription(String pos) {
        switch (pos) {
            case "NNG": return "일반명사";
            case "NNP": return "고유명사";
            case "NNB": return "의존명사";
            case "NP": return "대명사";
            case "VV": return "동사";
            case "VA": return "형용사";
            case "VX": return "보조용언";
            case "VCP": return "긍정지정사";
            case "VCN": return "부정지정사";
            case "MM": return "관형사";
            case "MAG": return "일반부사";
            case "MAJ": return "접속부사";
            case "IC": return "감탄사";
            case "JKS": return "주격조사";
            case "JKC": return "보격조사";
            case "JKG": return "관형격조사";
            case "JKO": return "목적격조사";
            case "JKB": return "부사격조사";
            case "JKV": return "호격조사";
            case "JKQ": return "인용격조사";
            case "JX": return "보조사";
            case "JC": return "접속조사";
            case "EP": return "선어말어미";
            case "EF": return "종결어미";
            case "EC": return "연결어미";
            case "ETN": return "명사형전성어미";
            case "ETM": return "관형형전성어미";
            case "XPN": return "체언접두사";
            case "XSN": return "명사파생접미사";
            case "XSV": return "동사파생접미사";
            case "XSA": return "형용사파생접미사";
            case "XR": return "어근";
            case "SF": return "마침표";
            case "SP": return "쉼표";
            case "SS": return "따옴표";
            case "SE": return "줄임표";
            case "SO": return "붙임표";
            case "SW": return "기타기호";
            case "SL": return "외국어";
            case "SH": return "한자";
            case "SN": return "숫자";
            default: return pos;
        }
    }

    /**
     * 문자열을 지정된 길이로 자르거나 패딩
     *
     * @param str 원본 문자열
     * @param maxLength 최대 길이
     * @return 조정된 문자열
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";

        // 한글 문자 수 계산 (한글은 2칸, 영문/숫자는 1칸으로 계산)
        int visualLength = 0;
        int charCount = 0;
        for (char c : str.toCharArray()) {
            visualLength += (c >= '가' && c <= '힣') ? 2 : 1;
            charCount++;
            if (visualLength >= maxLength) break;
        }

        String result = str.substring(0, Math.min(charCount, str.length()));
        if (str.length() > charCount) {
            result += "...";
            visualLength += 3;
        }

        // 패딩 추가
        int padding = maxLength - visualLength;
        if (padding > 0) {
            result += " ".repeat(padding);
        }

        return result;
    }
}
