package com.ssafy.meari.global.pipeline.videosaving.service;

import com.ssafy.meari.domain.admin.dto.ScriptCsvRowDto;
import com.ssafy.meari.domain.admin.util.CsvScriptParser;
import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.word.entity.SentenceWord;
import com.ssafy.meari.domain.word.entity.Word;
import com.ssafy.meari.domain.word.repository.SentenceWordRepository;
import com.ssafy.meari.domain.word.repository.WordRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import com.ssafy.meari.global.pipeline.videosaving.dto.HomonymWordDto;
import com.ssafy.meari.global.pipeline.videosaving.dto.WordMatchingInfo;
import com.ssafy.meari.global.pipeline.videosaving.dto.WordMatchingResultDto;
import com.ssafy.meari.global.pipeline.videosaving.homonym.service.HomonymDisambiguationService;
import com.ssafy.meari.global.pipeline.videosaving.nlp.dto.MorphemeAnalysisResponseDto;
import com.ssafy.meari.global.pipeline.videosaving.nlp.service.NlpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptSavingService {

	private final CsvScriptParser csvScriptParser;
	private final NlpService nlpService;
	private final HomonymDisambiguationService homonymDisambiguationService;
	private final SentenceRepository sentenceRepository;
	private final ContentRepository contentRepository;
	private final RoleRepository roleRepository;
	private final WordRepository wordRepository;
	private final SentenceWordRepository sentenceWordRepository;

	@Value("${pipeline.report.output-dir:./reports}")
	private String reportOutputDir;

	/**
	 * CSV 파일을 파싱하여 문장을 저장하고 형태소 분석을 수행하는 파이프라인
	 *
	 * @param file CSV 파일 (MultipartFile)
	 * @return 결과 CSV 파일 경로
	 */
	@Transactional
	public String processCsvFile(MultipartFile file) {
		log.debug("[Pipeline] CSV 파일 처리 시작 - 파일명: {}", file.getOriginalFilename());

		// 1. CSV 파싱
		List<ScriptCsvRowDto> rows = csvScriptParser.parse(file);
		log.info("[Pipeline] CSV 파싱 완료 - {}개 행", rows.size());

		// 2. Sentence 저장 및 형태소 분석 (매칭 결과 수집)
		List<WordMatchingResultDto> matchingResults = new ArrayList<>();
		List<Sentence> savedSentences = saveSentencesWithMorphemeAnalysis(rows, matchingResults);
		log.info("[Pipeline] 문장 저장 및 형태소 분석 완료 - {}개 문장, {}개 단어 매칭",
				savedSentences.size(), matchingResults.size());

		// 3. 매칭 결과 CSV 저장
		String csvPath = saveMatchingResultsCsv(matchingResults, file.getOriginalFilename());
		log.info("[Pipeline] 매칭 결과 CSV 저장 완료 - {}", csvPath);

		return csvPath;
	}

	/**
	 * 파일 시스템의 CSV 파일을 처리하는 파이프라인
	 *
	 * @param filePath CSV 파일 경로
	 * @return 결과 CSV 파일 경로
	 */
	@Transactional
	public String processCsvFile(Path filePath) {
		log.info("[Pipeline] CSV 파일 처리 시작 - 경로: {}", filePath);

		// 1. CSV 파싱
		List<ScriptCsvRowDto> rows = csvScriptParser.parseFile(filePath);
		log.info("[Pipeline] CSV 파싱 완료 - {}개 행", rows.size());

		// 2. Sentence 저장 및 형태소 분석 (매칭 결과 수집)
		List<WordMatchingResultDto> matchingResults = new ArrayList<>();
		List<Sentence> savedSentences = saveSentencesWithMorphemeAnalysis(rows, matchingResults);
		log.info("[Pipeline] 문장 저장 및 형태소 분석 완료 - {}개 문장, {}개 단어 매칭",
				savedSentences.size(), matchingResults.size());

		// 3. 매칭 결과 CSV 저장
		String csvPath = saveMatchingResultsCsv(matchingResults, filePath.getFileName().toString());
		log.info("[Pipeline] 매칭 결과 CSV 저장 완료 - {}", csvPath);

		return csvPath;
	}

	/**
	 * ScriptCsvRowDto 리스트를 Sentence로 변환하여 저장하고 형태소 분석 수행
	 *
	 * @param rows CSV 데이터 리스트
	 * @param matchingResults 매칭 결과를 수집할 리스트 (출력 파라미터)
	 * @return 저장된 Sentence 리스트
	 */
	private List<Sentence> saveSentencesWithMorphemeAnalysis(List<ScriptCsvRowDto> rows,
															 List<WordMatchingResultDto> matchingResults) {
		List<Sentence> savedSentences = new ArrayList<>();
		List<HomonymWordDto> allWordList = new ArrayList<>();  // 모든 단어 (SINGLE + HOMONYM)

		// 전체 스크립트 생성 (동음이의어 맥락 파악용)
		String fullScript = buildFullScript(rows);

		for (ScriptCsvRowDto row : rows) {
			log.info("[Pipeline] CSV 행 처리 - contentId: {}, roleId: {}, sequence: {}",
					row.getContentId(), row.getRoleId(), row.getSequence());

			// Content 조회
			Content content = contentRepository.findById(row.getContentId())
					.orElseThrow(() -> {
						log.error("[Pipeline] Content 조회 실패 - contentId: {}", row.getContentId());
						return new BusinessException(ErrorCode.NOT_FOUND_CONTENT);
					});

			// Role 조회
			Role role = roleRepository.findById(row.getRoleId())
					.orElseThrow(() -> {
						log.error("[Pipeline] Role 조회 실패 - roleId: {}", row.getRoleId());
						return new BusinessException(ErrorCode.NOT_FOUND_ROLE);
					});

			// Sentence 엔티티 생성
			Sentence sentence = Sentence.builder()
					.content(content)
					.role(role)
					.sequence(row.getSequence())
					.startTime(row.getStartTime())
					.endTime(row.getEndTime())
					.textKo(row.getTextKo())
					.textVn(row.getTextVn())
					.build();

			// 저장
			Sentence savedSentence = sentenceRepository.save(sentence);
			savedSentences.add(savedSentence);

			// 형태소 분석
			MorphemeAnalysisResponseDto morphemeResult = nlpService.analyzeMorphemes(row.getTextKo());
			log.debug("[Pipeline] 문장 ID: {}, 형태소 분석 결과: {}개",
					savedSentence.getSentenceId(),
					morphemeResult.getMorphemes().size());

			// 형태소 분석 결과로 Word 조회 (SINGLE도 LLM 검증 위해 리스트에 보관)
			linkWordsToSentence(savedSentence, morphemeResult, allWordList);
		}

		// 모든 단어를 LLM 처리 (SINGLE 검증 + HOMONYM 선택)
		List<WordMatchingInfo> validatedWords = new ArrayList<>();
		if (!allWordList.isEmpty()) {
			log.info("[Pipeline] LLM 검증 대상 단어 {}개 발견", allWordList.size());
			int singleCount = (int) allWordList.stream()
					.filter(dto -> dto.getHomonymWords().size() == 1)
					.count();
			int homonymCount = allWordList.size() - singleCount;
			log.info("  - SINGLE 검증: {}개, HOMONYM 선택: {}개", singleCount, homonymCount);

			log.info("[Pipeline] LLM 처리 시작");
			validatedWords = homonymDisambiguationService.processHomonyms(allWordList, fullScript);
			log.info("[Pipeline] LLM 처리 완료 - {}개 처리됨 ({}개 스킵됨)",
					validatedWords.size(), allWordList.size() - validatedWords.size());
		}

		// 검증된 단어들을 정렬 후 저장
		List<WordMatchingInfo> allWords = validatedWords;

		// 문장별로 그룹화하고 originalSequence로 정렬
		Map<Long, List<WordMatchingInfo>> wordsBySentence = allWords.stream()
				.collect(Collectors.groupingBy(
						info -> info.getSentence().getSentenceId(),
						Collectors.collectingAndThen(
								Collectors.toList(),
								list -> {
									list.sort(Comparator.comparingInt(WordMatchingInfo::getOriginalSequence));
									return list;
								}
						)
				));

		// 각 문장의 단어들을 sequence 1부터 재할당하여 저장
		for (Map.Entry<Long, List<WordMatchingInfo>> entry : wordsBySentence.entrySet()) {
			List<WordMatchingInfo> words = entry.getValue();
			int sequence = 1;

			for (WordMatchingInfo info : words) {
				// SentenceWord 저장
				SentenceWord sentenceWord = SentenceWord.builder()
						.sentence(info.getSentence())
						.word(info.getWord())
						.sequence(sequence++)
						.build();
				sentenceWordRepository.save(sentenceWord);

				// 매칭 결과 수집
				Word word = info.getWord();
				matchingResults.add(WordMatchingResultDto.builder()
						.sentenceSequence(info.getSentence().getSequence())
						.sentenceTextKo(info.getSentence().getTextKo())
						.wordKr(word.getWordKr())
						.definitionKr(word.getDefinitionKr())
						.wordVn(word.getWordVn())
						.definitionVn(word.getDefinitionVn())
						.matchType("VALIDATED")
						.build());
			}
		}

		// 문장 순서로 정렬
		matchingResults.sort(Comparator.comparingInt(WordMatchingResultDto::getSentenceSequence));

		log.info("[Pipeline] SentenceWord 저장 완료 - 총 {}개 단어", allWords.size());

		return savedSentences;
	}

	/**
	 * CSV 데이터로 전체 스크립트 생성
	 */
	private String buildFullScript(List<ScriptCsvRowDto> rows) {
		return rows.stream()
				.map(row -> String.format("[%d] %s", row.getSequence(), row.getTextKo()))
				.collect(Collectors.joining("\n"));
	}

	/**
	 * 형태소 분석 결과로 Word를 조회 (즉시 저장하지 않고 리스트에 보관)
	 * SINGLE/HOMONYM 모두 LLM 검증 대상으로 수집
	 *
	 * @param sentence 저장된 문장
	 * @param morphemeResult 형태소 분석 결과
	 * @param allWordList 모든 단어를 수집할 리스트 (LLM 검증용)
	 */
	private void linkWordsToSentence(Sentence sentence, MorphemeAnalysisResponseDto morphemeResult,
									 List<HomonymWordDto> allWordList) {
		List<MorphemeAnalysisResponseDto.Morpheme> morphemes = morphemeResult.getMorphemes();

		int wordSequence = 0;  // Word를 찾을 때마다 증가하는 카운터

		for (int i = 0; i < morphemes.size(); i++) {
			MorphemeAnalysisResponseDto.Morpheme morpheme = morphemes.get(i);
			String wordKr = morpheme.getText();

			// Word 테이블에서 조회
			List<Word> foundWords = wordRepository.findAllByWordKr(wordKr);

			if (foundWords.isEmpty()) {
				// Word가 없으면 스킵
				log.debug("[Pipeline] Word 없음 - wordKr: {}", wordKr);
				continue;
			}

			// Word를 찾았으므로 시퀀스 증가
			wordSequence++;

			// SINGLE/HOMONYM 구분 없이 모두 리스트에 추가 (LLM 검증용)
			HomonymWordDto wordDto = HomonymWordDto.builder()
					.sentence(sentence)
					.sentenceSequence(sentence.getSequence())
					.wordSequence(wordSequence)
					.wordKr(wordKr)
					.homonymWords(foundWords)
					.build();
			allWordList.add(wordDto);

			String type = foundWords.size() == 1 ? "SINGLE" : "HOMONYM";
			log.debug("[Pipeline] {} 단어 보관 - wordKr: {}, 후보: {}개, 단어 시퀀스: {}",
					type, wordKr, foundWords.size(), wordSequence);
		}
	}

	/**
	 * 매칭 결과를 CSV 파일로 저장
	 *
	 * @param matchingResults 매칭 결과 리스트
	 * @param sourceFileName 원본 파일명
	 * @return 저장된 CSV 파일 경로
	 */
	private String saveMatchingResultsCsv(List<WordMatchingResultDto> matchingResults, String sourceFileName) {
		try {
			// 출력 디렉토리 생성
			Path outputDir = Paths.get(reportOutputDir);
			Files.createDirectories(outputDir);

			// 파일명 생성: 원본파일명_matching_result_yyyyMMdd_HHmmss.csv
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			String baseName = sourceFileName != null ? sourceFileName.replaceAll("\\.[^.]+$", "") : "script";
			String fileName = String.format("%s_matching_result_%s.csv", baseName, timestamp);
			Path filePath = outputDir.resolve(fileName);

			// CSV 작성
			try (FileWriter writer = new FileWriter(filePath.toFile(), java.nio.charset.StandardCharsets.UTF_8)) {
				// BOM 추가 (Excel 호환)
				writer.write('\uFEFF');

				// 헤더
				writer.write("sentence_sequence,sentence_text_ko,word_kr,definition_kr,word_vn,definition_vn,match_type\n");

				// 데이터
				for (WordMatchingResultDto result : matchingResults) {
					writer.write(String.format("%d,%s,%s,%s,%s,%s,%s\n",
							result.getSentenceSequence(),
							escapeCsv(result.getSentenceTextKo()),
							escapeCsv(result.getWordKr()),
							escapeCsv(result.getDefinitionKr()),
							escapeCsv(result.getWordVn()),
							escapeCsv(result.getDefinitionVn()),
							result.getMatchType()
					));
				}
			}

			log.info("[Pipeline] 매칭 결과 CSV 저장 완료 - 경로: {}, 항목 수: {}", filePath, matchingResults.size());
			return filePath.toAbsolutePath().toString();

		} catch (IOException e) {
			log.error("[Pipeline] CSV 저장 실패: {}", e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * CSV 필드 이스케이프 처리
	 */
	private String escapeCsv(String value) {
		if (value == null) {
			return "";
		}
		// 쉼표, 따옴표, 줄바꿈이 포함된 경우 따옴표로 감싸기
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}
