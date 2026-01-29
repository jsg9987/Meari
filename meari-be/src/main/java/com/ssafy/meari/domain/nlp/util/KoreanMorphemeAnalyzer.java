package com.ssafy.meari.domain.nlp.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 간단한 한국어 형태소 분석 엔진
 * 정규식 기반으로 기본적인 토큰화와 품사 분류를 수행합니다.
 */
@Slf4j
public class KoreanMorphemeAnalyzer {

    // 한글 정규식 패턴
    private static final Pattern KOREAN_PATTERN = Pattern.compile("[가-힣]+");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");
    private static final Pattern SPACING_PATTERN = Pattern.compile("\\s+");

    // 한국어 조사 목록
    private static final Set<String> PARTICLES = Set.of(
            "이", "가", "을", "를", "에", "에게", "에게서", "에서", "으로", "로",
            "와", "과", "이랑", "랑", "하고", "만", "도", "까지", "조차", "부터"
    );

    // 한국어 어미 목록
    private static final Set<String> ENDINGS = Set.of(
            "다", "습니다", "네요", "어요", "아요", "어", "아", "으로", "려고",
            "길", "수", "더라", "라고", "군요", "나요", "버렸", "되었", "있다"
    );

    // 한국어 불용어 (조사, 조동사 등)
    private static final Set<String> STOPWORDS = Set.of(
            "이", "그", "저", "것", "것이", "수", "거", "더", "아니", "인", "있",
            "지", "못", "게", "까", "니", "네", "다"
    );

    /**
     * 문장을 토큰으로 분할합니다.
     *
     * @param text 분석할 문장
     * @return 토큰 목록 (형태: "단어(품사)")
     */
    public static List<String> tokenize(String text) {
        log.debug("[Analyzer] tokenize() 시작 - input: {}", text);

        if (text == null || text.isEmpty()) {
            log.warn("[Analyzer] 빈 텍스트");
            return new ArrayList<>();
        }

        List<String> tokens = new ArrayList<>();
        String cleanText = text.trim();
        log.debug("[Analyzer] cleanText: {}", cleanText);

        // 공백으로 먼저 분할
        String[] words = SPACING_PATTERN.split(cleanText);
        log.debug("[Analyzer] 공백 분할 후 word 개수: {}", words.length);

        for (String word : words) {
            if (word.isEmpty()) continue;

            log.debug("[Analyzer] 단어 처리 중: {}", word);
            // 한글, 숫자, 영문을 분리해서 처리
            List<String> analyzedTokens = analyzeWord(word);
            log.debug("[Analyzer] 단어 분석 결과: {}", analyzedTokens);
            tokens.addAll(analyzedTokens);
        }

        log.debug("[Analyzer] tokenize() 완료 - 총 {} 개 토큰", tokens.size());
        log.debug("[Analyzer] 최종 토큰 목록: {}", tokens);
        return tokens;
    }

    /**
     * 단어를 분석하여 형태소로 분리합니다.
     */
    private static List<String> analyzeWord(String word) {
        List<String> result = new ArrayList<>();

        // 한글과 숫자/영문이 섞여있는 경우 분리
        int i = 0;
        StringBuilder currentToken = new StringBuilder();
        String currentType = null;

        for (char c : word.toCharArray()) {
            String charType = getCharType(c);

            if (currentType == null) {
                currentType = charType;
                currentToken.append(c);
            } else if (charType.equals(currentType)) {
                currentToken.append(c);
            } else {
                // 타입이 바뀌면 이전 토큰 처리
                String token = currentToken.toString();
                if (!token.isEmpty()) {
                    result.add(classifyToken(token, currentType));
                }
                currentToken = new StringBuilder();
                currentToken.append(c);
                currentType = charType;
            }
        }

        // 마지막 토큰 처리
        if (currentToken.length() > 0) {
            String token = currentToken.toString();
            result.add(classifyToken(token, currentType));
        }

        return result;
    }

    /**
     * 문자의 타입을 반환합니다.
     */
    private static String getCharType(char c) {
        if (c >= 0xAC00 && c <= 0xD7A3) {
            return "KOREAN";
        } else if (Character.isDigit(c)) {
            return "NUMBER";
        } else if (Character.isLetter(c)) {
            return "ENGLISH";
        } else {
            return "SYMBOL";
        }
    }

    /**
     * 토큰을 분류하여 품사를 결정합니다.
     */
    private static String classifyToken(String token, String type) {
        if ("NUMBER".equals(type)) {
            return token + "(Number)";
        } else if ("ENGLISH".equals(type)) {
            return token + "(Foreign)";
        } else if ("SYMBOL".equals(type)) {
            return token + "(Punctuation)";
        } else if ("KOREAN".equals(type)) {
            return classifyKoreanToken(token);
        }
        return token + "(Unknown)";
    }

    /**
     * 한글 토큰의 품사를 분류합니다.
     */
    private static String classifyKoreanToken(String token) {
        // 조사 확인
        if (PARTICLES.contains(token)) {
            return token + "(Josa)";
        }

        // 어미 확인
        if (ENDINGS.contains(token)) {
            return token + "(Eomi)";
        }

        // 기본적으로 명사로 분류
        return token + "(Noun)";
    }

    /**
     * 명사를 추출합니다.
     */
    public static List<String> extractNouns(String text) {
        log.debug("[Analyzer] extractNouns() 시작 - input: {}", text);
        List<String> nouns = new ArrayList<>();
        List<String> tokens = tokenize(text);
        log.debug("[Analyzer] extractNouns - tokenize 결과: {} 개", tokens.size());

        for (String token : tokens) {
            if (token.contains("(Noun)")) {
                String noun = token.replace("(Noun)", "");
                if (!STOPWORDS.contains(noun)) {
                    log.debug("[Analyzer] 명사 추출: {}", noun);
                    nouns.add(noun);
                }
            }
        }

        log.debug("[Analyzer] extractNouns() 완료 - 추출된 명사: {}", nouns);
        return nouns;
    }

    /**
     * 특정 품사의 단어를 추출합니다.
     */
    public static List<String> extractByPos(String text, String pos) {
        log.debug("[Analyzer] extractByPos() 시작 - input: {}, pos: {}", text, pos);
        List<String> words = new ArrayList<>();
        List<String> tokens = tokenize(text);
        log.debug("[Analyzer] extractByPos - tokenize 결과: {} 개", tokens.size());

        String posTag = "(" + pos + ")";
        for (String token : tokens) {
            if (token.contains(posTag)) {
                String word = token.replace(posTag, "");
                log.debug("[Analyzer] {} 품사 단어 추출: {}", pos, word);
                words.add(word);
            }
        }

        log.debug("[Analyzer] extractByPos() 완료 - 추출된 단어: {}", words);
        return words;
    }
}
