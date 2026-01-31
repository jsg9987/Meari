package com.ssafy.meari.global.pipeline.videosaving.homonym.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.domain.word.entity.SentenceWord;
import com.ssafy.meari.domain.word.entity.Word;
import com.ssafy.meari.domain.word.repository.SentenceWordRepository;
import com.ssafy.meari.global.pipeline.videosaving.dto.HomonymWordDto;
import com.ssafy.meari.global.pipeline.videosaving.dto.WordMatchingResultDto;
import com.ssafy.meari.global.pipeline.videosaving.openai.service.OpenAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomonymDisambiguationService {

	private final OpenAiService openAiService;
	private final SentenceWordRepository sentenceWordRepository;
	private final ObjectMapper objectMapper;

	private static final String SYSTEM_PROMPT = """
			당신은 한국어 동음이의어를 구별하는 전문가입니다.

			## 필수 분석 순서 (반드시 이 순서로 진행하세요)
			1. **[1단계] 전체 스크립트 읽기**: 먼저 제공된 전체 스크립트를 처음부터 끝까지 읽고 전체적인 주제와 흐름을 파악하세요
			2. **[2단계] 맥락 이해**: 스크립트가 어떤 상황을 다루는지 (뉴스, 대화, 설명 등) 전체 맥락을 이해하세요
			3. **[3단계] 개별 단어 분석**: 각 동음이의어가 등장하는 문장을 다시 읽고, 전체 맥락 속에서 어떤 의미로 사용되었는지 판단하세요

			## 단어 분석 기준
			1. 해당 단어가 문장에서 **어떤 문법적 역할**을 하는가? (명사, 동사, 조사 등)
			2. 전체 스크립트의 **주제와 상황**에 비추어 어떤 의미가 적절한가?
			3. 앞뒤 문맥에서 단어가 **실제로 어떻게 사용**되는가?
			4. 각 후보 뜻을 문장에 대입했을 때 **자연스럽고 논리적**인가?

			## 분석 예시
			**예시 1:**
			- 전체 맥락: 채소 가격 상승에 관한 뉴스
			- 문장: "가격이 눈에 띄게 올랐습니다"
			- 단어: "오르다"
			- 후보: 1. 높은 곳으로 이동하다 (물리적), 2. 수치나 정도가 증가하다
			- 분석: 가격은 물리적으로 이동할 수 없고, 뉴스 맥락상 "가격 상승"을 의미
			- 선택: 2번 (증가하다)

			**예시 2:**
			- 전체 맥락: 시장 취재 뉴스
			- 문장: "장을 보러 나온 시민들"
			- 단어: "나오다"
			- 후보: 1. 밖으로 나가다, 외출하다, 2. 목적지가 보이다
			- 분석: "장을 보러"는 목적을 나타내고, "나온"은 외출의 의미
			- 선택: 1번 (외출하다)

			## 중요: 응답 규칙
			1. 반드시 JSON 배열로만 응답하세요
			2. 배열의 길이는 반드시 동음이의어 개수와 동일해야 합니다
			3. 각 숫자는 해당 동음이의어의 후보 번호입니다 (1부터 시작)
			4. 설명, 마크다운, 추가 텍스트 없이 오직 JSON 배열만 출력하세요

			## 응답 예시
			동음이의어가 5개라면: [2, 1, 3, 1, 2]
			동음이의어가 3개라면: [1, 2, 1]
			""";

	/**
	 * 동음이의어 리스트를 처리하여 올바른 Word를 선택하고 SentenceWord 연결
	 * 한 번의 API 호출로 모든 동음이의어를 처리
	 *
	 * @param homonymList 동음이의어 리스트
	 * @param fullScript 전체 스크립트 (맥락 파악용)
	 * @return 매칭 결과 리스트
	 */
	@Transactional
	public List<WordMatchingResultDto> processHomonyms(List<HomonymWordDto> homonymList, String fullScript) {
		log.info("[Homonym] 동음이의어 처리 시작 - {}개", homonymList.size());

		List<WordMatchingResultDto> matchingResults = new ArrayList<>();

		if (homonymList.isEmpty()) {
			log.info("[Homonym] 처리할 동음이의어 없음");
			return matchingResults;
		}

		// 사용자 프롬프트 생성
		String userPrompt = buildUserPrompt(homonymList, fullScript);
		log.debug("[Homonym] LLM 요청 - 동음이의어 {}개", homonymList.size());

		// OpenAI API 호출 (한 번만)
		String response = openAiService.chat(SYSTEM_PROMPT, userPrompt);
		log.info("[Homonym] LLM 응답: {}", response);

		if (response == null || response.isBlank()) {
			log.warn("[Homonym] LLM 응답이 비어있음");
			return new ArrayList<>();
		}

		// 응답에서 선택 배열 파싱
		List<Integer> selections = parseResponse(response.trim());
		log.info("[Homonym] 파싱된 선택 배열: {} ({}개)", selections, selections.size());

		if (selections.size() != homonymList.size()) {
			log.warn("[Homonym] LLM 응답 개수 불일치 - 요청: {}, 응답: {} (일부만 처리합니다)",
					homonymList.size(), selections.size());
		}

		// 응답 개수가 부족해도 있는 만큼은 처리
		int processableCount = Math.min(selections.size(), homonymList.size());

		int processedCount = 0;

		log.info("[Homonym] AI 선택 결과 ({}개 처리):", processableCount);
		for (int i = 0; i < processableCount; i++) {
			HomonymWordDto homonymDto = homonymList.get(i);
			int selectedIndex = selections.get(i);
			List<Word> candidates = homonymDto.getHomonymWords();

			try {
				if (selectedIndex >= 1 && selectedIndex <= candidates.size()) {
					Word selectedWord = candidates.get(selectedIndex - 1);

					// 후보 목록과 선택 결과 로그
					StringBuilder candidatesLog = new StringBuilder();
					for (int j = 0; j < candidates.size(); j++) {
						if (j == selectedIndex - 1) {
							candidatesLog.append("[✓").append(j + 1).append(". ").append(candidates.get(j).getDefinitionKr()).append("] ");
						} else {
							candidatesLog.append(j + 1).append(". ").append(candidates.get(j).getDefinitionKr()).append(" / ");
						}
					}
					log.info("  - '{}' → {} (문장[{}])",
							homonymDto.getWordKr(), candidatesLog.toString().trim(), homonymDto.getSentence().getSequence());

					// SentenceWord 연결
					SentenceWord sentenceWord = SentenceWord.builder()
							.sentence(homonymDto.getSentence())
							.word(selectedWord)
							.sequence(homonymDto.getSentenceSequence())
							.build();
					sentenceWordRepository.save(sentenceWord);

					// 매칭 결과 수집
					matchingResults.add(WordMatchingResultDto.builder()
							.sentenceSequence(homonymDto.getSentence().getSequence())
							.sentenceTextKo(homonymDto.getSentence().getTextKo())
							.wordKr(selectedWord.getWordKr())
							.definitionKr(selectedWord.getDefinitionKr())
							.wordVn(selectedWord.getWordVn())
							.definitionVn(selectedWord.getDefinitionVn())
							.matchType("HOMONYM")
							.build());

					processedCount++;
				} else {
					log.warn("[Homonym] 선택 범위 초과 - wordKr: {}, 선택: {}, 후보 수: {}",
							homonymDto.getWordKr(), selectedIndex, candidates.size());
				}
			} catch (Exception e) {
				log.error("[Homonym] 동음이의어 처리 실패 - wordKr: {}, error: {}",
						homonymDto.getWordKr(), e.getMessage());
			}
		}

		log.info("[Homonym] 동음이의어 처리 완료 - {}개 중 {}개 처리",
				homonymList.size(), processedCount);

		return matchingResults;
	}

	/**
	 * 사용자 프롬프트 생성 - 전체 스크립트와 모든 동음이의어 포함
	 */
	private String buildUserPrompt(List<HomonymWordDto> homonymList, String fullScript) {
		StringBuilder sb = new StringBuilder();

		sb.append("## 전체 스크립트\n");
		sb.append(fullScript).append("\n\n");

		sb.append("## 동음이의어 목록\n");
		sb.append("아래 동음이의어들에 대해 스크립트 맥락에 맞는 뜻 번호를 순서대로 선택해주세요.\n\n");

		for (int i = 0; i < homonymList.size(); i++) {
			HomonymWordDto dto = homonymList.get(i);
			sb.append("### ").append(i + 1).append(". '").append(dto.getWordKr()).append("'\n");
			sb.append("등장 위치: [").append(dto.getSentence().getSequence()).append("] ")
					.append(dto.getSentence().getTextKo()).append("\n");
			sb.append("후보:\n");

			List<Word> candidates = dto.getHomonymWords();
			for (int j = 0; j < candidates.size(); j++) {
				Word word = candidates.get(j);
				sb.append("  ").append(j + 1).append(". ").append(word.getDefinitionKr()).append("\n");
			}
			sb.append("\n");
		}

		sb.append("위 ").append(homonymList.size()).append("개 동음이의어에 대해 선택한 번호를 JSON 배열로 응답하세요.");
		sb.append("\n예시: [2, 1, 3]");

		return sb.toString();
	}

	/**
	 * LLM 응답에서 JSON 배열 파싱
	 */
	private List<Integer> parseResponse(String response) {
		try {
			// JSON 배열 부분만 추출 (앞뒤 텍스트 제거)
			int startIdx = response.indexOf('[');
			int endIdx = response.lastIndexOf(']');

			if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
				log.warn("[Homonym] JSON 배열을 찾을 수 없음 - 응답: {}", response);
				return new ArrayList<>();
			}

			String jsonArray = response.substring(startIdx, endIdx + 1);
			return objectMapper.readValue(jsonArray, new TypeReference<List<Integer>>() {});

		} catch (JsonProcessingException e) {
			log.error("[Homonym] JSON 파싱 실패 - 응답: {}, error: {}", response, e.getMessage());
			return new ArrayList<>();
		}
	}
}
