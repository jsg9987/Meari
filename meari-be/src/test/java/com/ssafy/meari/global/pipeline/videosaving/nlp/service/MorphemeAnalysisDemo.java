package com.ssafy.meari.global.pipeline.videosaving.nlp.service;

import com.ssafy.meari.global.pipeline.videosaving.nlp.dto.MorphemeAnalysisResponseDto;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * 형태소분석 데모 - main 메서드로 직접 실행 가능
 */
public class MorphemeAnalysisDemo {

	public static void main(String[] args) throws Exception {
		// UTF-8 인코딩으로 콘솔 출력 설정
		System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

		NlpService nlpService = new NlpServiceImpl();

		System.out.println("═══════════════════════════════════════════════════════════");
		System.out.println("         KOMORAN 형태소 분석 데모");
		System.out.println("═══════════════════════════════════════════════════════════\n");

		// 테스트 1: 단순 문장
		testMorphemeAnalysis(nlpService, "넌 꿈이 뭐니?");

		// 테스트 2: 복잡한 문장
		testMorphemeAnalysis(nlpService, "작정이란 게 계획이란 게 너한테 있긴 하니?");

		// 테스트 3: 형용사 추출
		testAdjectiveExtraction(nlpService, "넌 깨끗하고 예뻐?");

		// 테스트 4: 명사 추출
		testNounExtraction(nlpService, "발레 하는 사람들 발은 발톱이 수십 번이나 빠진다");

		// 테스트 5: 실제 스크립트 문장들
		testScriptSentences(nlpService);
	}

	private static void testMorphemeAnalysis(NlpService nlpService, String text) {
		System.out.println("\n[테스트] 형태소 분석");
		System.out.println("─────────────────────────────────────────────────────────");
		System.out.println("원문: " + text);
		System.out.println();

		MorphemeAnalysisResponseDto result = nlpService.analyzeMorphemes(text);

		System.out.println("분석 결과 (" + result.getMorphemes().size() + "개 형태소):");
		System.out.println();

		int index = 1;
		for (MorphemeAnalysisResponseDto.Morpheme morpheme : result.getMorphemes()) {
			System.out.printf("  %2d. %-6s  →  %s (품사: %s, 신뢰도: %.1f)%n",
					index++,
					morpheme.getText(),
					morpheme.getText(),
					morpheme.getPos(),
					morpheme.getConfidence());
		}
	}

	private static void testAdjectiveExtraction(NlpService nlpService, String text) {
		System.out.println("\n[테스트] 형용사(VA) 추출");
		System.out.println("─────────────────────────────────────────────────────────");
		System.out.println("원문: " + text);
		System.out.println();

		var morphemes = nlpService.analyzeMorphemes(text);
		System.out.println("전체 형태소:");
		morphemes.getMorphemes().forEach(m ->
				System.out.println("  - " + m.getText() + " (" + m.getPos() + ")")
		);

		System.out.println();
		var adjectives = nlpService.extractByPos(text, "VA");
		System.out.println("추출된 형용사 (VA): " + adjectives);
	}

	private static void testNounExtraction(NlpService nlpService, String text) {
		System.out.println("\n[테스트] 명사(N) 추출");
		System.out.println("─────────────────────────────────────────────────────────");
		System.out.println("원문: " + text);
		System.out.println();

		var morphemes = nlpService.analyzeMorphemes(text);
		System.out.println("전체 형태소:");
		morphemes.getMorphemes().forEach(m ->
				System.out.println("  - " + m.getText() + " (" + m.getPos() + ")")
		);

		System.out.println();
		var nouns = nlpService.extractNouns(text);
		System.out.println("추출된 명사: " + nouns);
	}

	private static void testScriptSentences(NlpService nlpService) {
		System.out.println("\n[테스트] 실제 스크립트 문장들");
		System.out.println("─────────────────────────────────────────────────────────");

		String[] scriptSentences = {
				"넌 꿈이 뭐니?",
				"앞으로 어떻게 살 작정이야?",
				"발레가 계속 하고 싶긴 해?",
				"발레 하는 사람들 발은 발톱이 수십 번이나 빠지고 뼈가 이리저리 튀어나와 사람 발로도 짐승 발로도 안 보인다."
		};

		for (String sentence : scriptSentences) {
			System.out.println("\n원문: " + sentence);

			var result = nlpService.analyzeMorphemes(sentence);
			System.out.print("형태소: ");
			System.out.println(
					result.getMorphemes().stream()
							.map(m -> m.getText() + "(" + m.getPos() + ")")
							.toList()
			);

			var nouns = nlpService.extractNouns(sentence);
			if (!nouns.isEmpty()) {
				System.out.println("명사: " + nouns);
			}

			var verbs = nlpService.extractByPos(sentence, "VV");
			if (!verbs.isEmpty()) {
				System.out.println("동사: " + verbs);
			}
		}
	}
}
