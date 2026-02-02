package com.ssafy.meari.global.pipeline.videosaving.homonym.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meari.domain.word.entity.Word;
import com.ssafy.meari.global.pipeline.videosaving.anthropic.service.AnthropicService;
import com.ssafy.meari.global.pipeline.videosaving.anthropic.dto.HomonymWordDto;
import com.ssafy.meari.global.pipeline.videosaving.anthropic.dto.WordMatchingInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomonymDisambiguationService {

	private final AnthropicService anthropicService;
	private final ObjectMapper objectMapper;

	private static final String SYSTEM_PROMPT = """
			당신은 한국어 단어 매칭을 검증하는 전문가입니다.
			제공된 뜻풀이(정의)는 신뢰성 높은 사전 데이터이지만, 형태소 분석 오류로 인해
			잘못된 단어가 추출되었을 수 있습니다. 따라서 뜻풀이와 실제 문맥의 일치도를 엄격하게 검증하세요.

			## 필수 분석 순서 (반드시 이 순서로 진행하세요)
			1. **[1단계] 전체 스크립트 읽기**: 먼저 제공된 전체 스크립트를 처음부터 끝까지 읽고 전체적인 주제와 흐름을 파악하세요
			2. **[2단계] 맥락 이해**: 스크립트가 어떤 상황을 다루는지 (뉴스, 대화, 설명 등) 전체 맥락을 이해하세요
			3. **[3단계] 형태소 분석 검증**: 형태소 분석기가 정말 올바른 단어를 추출했는지 확인하세요
			4. **[4단계] 뜻풀이 검증**: 각 후보의 뜻풀이가 실제 문맥과 일치하는지 엄격하게 검증하세요

			## 단어 분석 기준 (순서대로 확인하세요)
			1. **형태소 분석 오류 의심 사항 확인**:
			   - "사는 게" → "게"(동물 뜻풀이)는 형태소 분석 오류일 가능성 높음
			   - "준 건가요" → "걸다"(동사)는 형태소 분석 오류일 가능성 높음
			   - 의존명사, 조사, 어미는 보통 형태소 분석기가 잘못 인식함

			2. **뜻풀이와 문장의 일치도 검증**:
			   - 후보 뜻을 문장에 대입했을 때 **말이 되는가?**
			   - 문맥상 **완전히 무관한 뜻**은 없는가?
			   - 예: "채소를 사는 게 부담" → "게"(동물)을 대입 → "채소를 사는 동물 부담" (말이 안 됨 → null)

			3. **물리적 vs 추상적 의미 구분**:
			   - "가격이 올랐습니다" → "올라" (물리적 이동) vs (수치 증가)
			   - 가격은 물리적으로 이동할 수 없으므로 "수치 증가" 선택

			4. **각 후보 뜻을 문장에 '실제로 대입'하여 검증**:
			   - 최종 선택 전에 매 후보마다 이 과정을 거쳐야 함

			## 분석 예시 (매우 상세함)
			**예시 1: 뜻풀이 불일치로 인한 거부**
			- 문장: "네, 상인들과 소비자 모두 가격 상승을 체감하고 있다고 말합니다"
			- 단어: "가격"
			- 후보 1: "손이나 주먹, 몽둥이 등으로 치거나 때림" ← 이건 뭐지? "때리다"의 뜻이 아닌가?
			- 검증: "가격(때리다) 상승을 체감" → 문맥상 말이 안 됨 → null (거부)

			**예시 2: 단순 형태소 분석 오류 감지**
			- 문장: "예전보다 채소를 사는 게 부담스럽다"
			- 단어: "게"
			- 후보 1: "온몸이 단단한 껍질로 싸여 있으며 열 개의 발이 있는 동물"
			- 형태소 분석 검증: "사는 게" 표현에서 "게"는 동물이 아니라 "것이"의 구어체
			- 검증: "채소를 사는 동물 부담" → 말이 안 됨 → null (거부)

			**예시 3: 올바른 선택**
			- 문장: "장마가 길어지면서 채소 가격이 전반적으로 오른 모습입니다"
			- 단어: "오르다"
			- 후보 1: "높은 곳으로 이동하다"
			- 후보 2: "값, 수치, 온도 등이 이전보다 많아지거나 높아지다"
			- 검증: "가격이 오른" 문맥에서 후보 1을 대입 → "가격이 높은 곳으로 이동" (말이 안 됨)
			- 검증: "가격이 오른" 문맥에서 후보 2를 대입 → "가격이 수치상 증가" (말이 됨) → 선택: 2

			## 매우 중요: 응답 규칙
			1. 뜻풀이가 문맥과 **명백하게 불일치**하면 망설이지 말고 null로 거부하세요
			2. 후보 뜻이 "완전히 다른 단어의 뜻"처럼 보이면 형태소 분석 오류일 가능성 높음 → null
			3. 반드시 JSON 배열로만 응답하세요 (설명, 주석 없음)
			4. 배열의 길이는 반드시 단어 개수와 동일해야 합니다

			## 응답 예시
			[2, null, 1, 3, null, 1]
			""";

	/**
	 * 동음이의어 리스트를 처리하여 올바른 Word를 선택
	 * 한 번의 API 호출로 모든 동음이의어를 처리
	 * (즉시 저장하지 않고 WordMatchingInfo 리스트로 반환)
	 *
	 * @param homonymList 동음이의어 리스트
	 * @param fullScript 전체 스크립트 (맥락 파악용)
	 * @return WordMatchingInfo 리스트
	 */
	@Transactional
	public List<WordMatchingInfo> processHomonyms(List<HomonymWordDto> homonymList, String fullScript) {
		log.info("[Homonym] 동음이의어 처리 시작 - {}개", homonymList.size());

		List<WordMatchingInfo> wordMatchingInfos = new ArrayList<>();

		if (homonymList.isEmpty()) {
			return wordMatchingInfos;
		}

		// 사용자 프롬프트 생성
		String userPrompt = buildUserPrompt(homonymList, fullScript);

		// Claude API 호출 (한 번만)
		String response = anthropicService.chat(SYSTEM_PROMPT, userPrompt);

		if (response == null || response.isBlank()) {
			log.warn("[Homonym] LLM 응답이 비어있음");
			return new ArrayList<>();
		}

		// 응답에서 선택 배열 파싱
		List<Integer> selections = parseResponse(response.trim());

		if (selections.size() != homonymList.size()) {
			log.warn("[Homonym] LLM 응답 개수 불일치 - 요청: {}, 응답: {} (일부만 처리합니다)",
				homonymList.size(), selections.size());
		}

		// 응답 개수가 부족해도 있는 만큼은 처리
		int processableCount = Math.min(selections.size(), homonymList.size());

		int processedCount = 0;
		int skippedCount = 0;
		for (int i = 0; i < processableCount; i++) {
			HomonymWordDto homonymDto = homonymList.get(i);
			Integer selectedIndex = selections.get(i);  // null 가능
			List<Word> candidates = homonymDto.getHomonymWords();

			try {
				// null인 경우 스킵 (LLM이 문맥상 무관하다고 판단)
				if (selectedIndex == null) {
					log.info("  - '{}' → [SKIP] 문맥상 무관 (문장[{}])",
						homonymDto.getWordKr(), homonymDto.getSentence().getSequence());
					skippedCount++;
					continue;
				}

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

					String type = candidates.size() == 1 ? "SINGLE" : "HOMONYM";
					log.info("  - '{}' ({}) → {} (문장[{}])",
						homonymDto.getWordKr(), type, candidatesLog.toString().trim(),
						homonymDto.getSentence().getSequence());

					// WordMatchingInfo 생성 (즉시 저장하지 않음)
					WordMatchingInfo matchingInfo = WordMatchingInfo.builder()
						.sentence(homonymDto.getSentence())
						.word(selectedWord)
						.originalSequence(homonymDto.getWordSequence())
						.build();
					wordMatchingInfos.add(matchingInfo);

					processedCount++;
				} else {
					log.warn("[Homonym] 선택 범위 초과 - wordKr: {}, 선택: {}, 후보 수: {}",
						homonymDto.getWordKr(), selectedIndex, candidates.size());
					skippedCount++;
				}
			} catch (Exception e) {
				log.error("[Homonym] 단어 처리 실패 - wordKr: {}, error: {}",
					homonymDto.getWordKr(), e.getMessage());
				skippedCount++;
			}
		}

		log.info("[Homonym] LLM 검증 완료 - 전체: {}개, 통과: {}개, 스킵: {}개",
			homonymList.size(), processedCount, skippedCount);

		return wordMatchingInfos;
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

		sb.append("위 ").append(homonymList.size()).append("개 단어에 대해 선택/검증 결과를 JSON 배열로 응답하세요.");
		sb.append("\n예시: [2, null, 1, 3, 1, null]  (null은 문맥상 무관한 단어)");

		return sb.toString();
	}

	/**
	 * LLM 응답에서 JSON 배열 파싱 (null 값 허용)
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
			// null 값을 허용하는 List<Integer> 파싱
			return objectMapper.readValue(jsonArray, new TypeReference<List<Integer>>() {});

		} catch (JsonProcessingException e) {
			log.error("[Homonym] JSON 파싱 실패 - 응답: {}, error: {}", response, e.getMessage());
			return new ArrayList<>();
		}
	}
}
