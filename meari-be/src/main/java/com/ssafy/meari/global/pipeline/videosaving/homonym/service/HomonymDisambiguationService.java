package com.ssafy.meari.global.pipeline.videosaving.homonym.service;

import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.word.entity.SentenceWord;
import com.ssafy.meari.domain.word.entity.Word;
import com.ssafy.meari.domain.word.repository.SentenceWordRepository;
import com.ssafy.meari.global.pipeline.videosaving.dto.HomonymWordDto;
import com.ssafy.meari.global.pipeline.videosaving.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomonymDisambiguationService {

	private final OpenAiService openAiService;
	private final SentenceWordRepository sentenceWordRepository;

	private static final String SYSTEM_PROMPT = """
			당신은 한국어 동음이의어를 구별하는 전문가입니다.
			주어진 문장의 맥락을 분석하여, 동음이의어 중 가장 적절한 단어를 선택해주세요.

			응답 형식:
			- 반드시 숫자만 응답하세요 (예: 1, 2, 3)
			- 선택한 단어의 번호만 출력하세요
			- 설명이나 다른 텍스트 없이 숫자만 응답하세요
			""";

	/**
	 * 동음이의어 리스트를 처리하여 올바른 Word를 선택하고 SentenceWord 연결
	 *
	 * @param homonymList 동음이의어 리스트
	 * @return 처리된 동음이의어 수
	 */
	@Transactional
	public int processHomonyms(List<HomonymWordDto> homonymList) {
		log.info("[Homonym] 동음이의어 처리 시작 - {}개", homonymList.size());

		int processedCount = 0;

		for (HomonymWordDto homonymDto : homonymList) {
			try {
				Word selectedWord = disambiguate(homonymDto);

				if (selectedWord != null) {
					// SentenceWord 연결
					SentenceWord sentenceWord = SentenceWord.builder()
							.sentence(homonymDto.getSentence())
							.word(selectedWord)
							.sequence(0) // 동음이의어는 시퀀스 정보가 없으므로 0으로 설정
							.build();
					sentenceWordRepository.save(sentenceWord);

					log.debug("[Homonym] SentenceWord 연결 완료 - sentenceId: {}, wordId: {}, wordKr: {}",
							homonymDto.getSentence().getSentenceId(),
							selectedWord.getWordId(),
							homonymDto.getWordKr());

					processedCount++;
				}
			} catch (Exception e) {
				log.error("[Homonym] 동음이의어 처리 실패 - wordKr: {}, error: {}",
						homonymDto.getWordKr(), e.getMessage());
			}
		}

		log.info("[Homonym] 동음이의어 처리 완료 - {}개 중 {}개 처리",
				homonymList.size(), processedCount);

		return processedCount;
	}

	/**
	 * 단일 동음이의어를 LLM으로 구별
	 *
	 * @param homonymDto 동음이의어 정보
	 * @return 선택된 Word (실패 시 null)
	 */
	private Word disambiguate(HomonymWordDto homonymDto) {
		Sentence sentence = homonymDto.getSentence();
		String wordKr = homonymDto.getWordKr();
		List<Word> candidates = homonymDto.getHomonymWords();

		// 사용자 프롬프트 생성
		String userPrompt = buildUserPrompt(sentence.getTextKo(), wordKr, candidates);

		log.debug("[Homonym] LLM 요청 - wordKr: {}, 후보 수: {}", wordKr, candidates.size());

		// OpenAI API 호출
		String response = openAiService.chat(SYSTEM_PROMPT, userPrompt);

		if (response == null || response.isBlank()) {
			log.warn("[Homonym] LLM 응답이 비어있음 - wordKr: {}", wordKr);
			return null;
		}

		// 응답에서 숫자 추출
		try {
			int selectedIndex = parseResponse(response.trim());

			if (selectedIndex >= 1 && selectedIndex <= candidates.size()) {
				Word selectedWord = candidates.get(selectedIndex - 1);
				log.debug("[Homonym] LLM 선택 - wordKr: {}, 선택: {} ({})",
						wordKr, selectedIndex, selectedWord.getDefinitionKr());
				return selectedWord;
			} else {
				log.warn("[Homonym] LLM 응답 범위 초과 - wordKr: {}, 응답: {}, 후보 수: {}",
						wordKr, response, candidates.size());
				return null;
			}
		} catch (NumberFormatException e) {
			log.warn("[Homonym] LLM 응답 파싱 실패 - wordKr: {}, 응답: {}", wordKr, response);
			return null;
		}
	}

	/**
	 * 사용자 프롬프트 생성
	 */
	private String buildUserPrompt(String sentenceText, String wordKr, List<Word> candidates) {
		StringBuilder sb = new StringBuilder();
		sb.append("문장: ").append(sentenceText).append("\n\n");
		sb.append("동음이의어: ").append(wordKr).append("\n\n");
		sb.append("후보 단어 목록:\n");

		for (int i = 0; i < candidates.size(); i++) {
			Word word = candidates.get(i);
			sb.append(i + 1).append(". ")
					.append(word.getWordKr())
					.append(" - ")
					.append(word.getDefinitionKr())
					.append("\n");
		}

		sb.append("\n위 문장에서 '").append(wordKr).append("'에 해당하는 단어 번호를 선택하세요.");

		return sb.toString();
	}

	/**
	 * LLM 응답에서 숫자 추출
	 */
	private int parseResponse(String response) {
		// 숫자만 추출
		String numberOnly = response.replaceAll("[^0-9]", "");
		if (numberOnly.isEmpty()) {
			throw new NumberFormatException("No number found in response: " + response);
		}
		return Integer.parseInt(numberOnly);
	}
}
