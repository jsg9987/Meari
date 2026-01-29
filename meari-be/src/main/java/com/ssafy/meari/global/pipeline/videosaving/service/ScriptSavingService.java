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
import com.ssafy.meari.global.pipeline.videosaving.homonym.service.HomonymDisambiguationService;
import com.ssafy.meari.global.pipeline.videosaving.nlp.dto.MorphemeAnalysisResponseDto;
import com.ssafy.meari.global.pipeline.videosaving.nlp.service.NlpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

	// 동음이의어 리스트
	private List<HomonymWordDto> homonymList = new ArrayList<>();

	/**
	 * CSV 파일을 파싱하여 문장을 저장하고 형태소 분석을 수행하는 파이프라인
	 *
	 * @param file CSV 파일 (MultipartFile)
	 * @return 저장된 문장 수
	 */
	@Transactional
	public int processCsvFile(MultipartFile file) {
		log.debug("[Pipeline] CSV 파일 처리 시작 - 파일명: {}", file.getOriginalFilename());

		// 1. CSV 파싱
		List<ScriptCsvRowDto> rows = csvScriptParser.parse(file);
		log.info("[Pipeline] CSV 파싱 완료 - {}개 행", rows.size());

		// 2. Sentence 저장 및 형태소 분석
		List<Sentence> savedSentences = saveSentencesWithMorphemeAnalysis(rows);
		log.info("[Pipeline] 문장 저장 및 형태소 분석 완료 - {}개 문장", savedSentences.size());

		return savedSentences.size();
	}

	/**
	 * 파일 시스템의 CSV 파일을 처리하는 파이프라인
	 *
	 * @param filePath CSV 파일 경로
	 * @return 저장된 문장 수
	 */
	@Transactional
	public int processCsvFile(Path filePath) {
		log.info("[Pipeline] CSV 파일 처리 시작 - 경로: {}", filePath);

		// 1. CSV 파싱
		List<ScriptCsvRowDto> rows = csvScriptParser.parseFile(filePath);
		log.info("[Pipeline] CSV 파싱 완료 - {}개 행", rows.size());

		// 2. Sentence 저장 및 형태소 분석
		List<Sentence> savedSentences = saveSentencesWithMorphemeAnalysis(rows);
		log.info("[Pipeline] 문장 저장 및 형태소 분석 완료 - {}개 문장", savedSentences.size());

		return savedSentences.size();
	}

	/**
	 * ScriptCsvRowDto 리스트를 Sentence로 변환하여 저장하고 형태소 분석 수행
	 */
	private List<Sentence> saveSentencesWithMorphemeAnalysis(List<ScriptCsvRowDto> rows) {
		List<Sentence> savedSentences = new ArrayList<>();
		homonymList.clear(); // 동음이의어 리스트 초기화

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

			// 형태소 분석 결과로 Word 조회 및 SentenceWord 연결
			linkWordsToSentence(savedSentence, morphemeResult);
		}

		// 동음이의어 LLM 처리
		if (!homonymList.isEmpty()) {
			log.info("[Pipeline] 동음이의어 {}개 발견 - 목록:", homonymList.size());
			for (HomonymWordDto dto : homonymList) {
				log.info("  - '{}' (후보 {}개) in 문장[{}]: {}",
						dto.getWordKr(),
						dto.getHomonymWords().size(),
						dto.getSentence().getSequence(),
						dto.getSentence().getTextKo());
			}
			log.info("[Pipeline] LLM 처리 시작");
			int processedCount = homonymDisambiguationService.processHomonyms(homonymList, fullScript);
			log.info("[Pipeline] 동음이의어 LLM 처리 완료 - {}개 처리됨", processedCount);
		}

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
	 * 형태소 분석 결과로 Word를 조회하고 SentenceWord로 연결
	 * 동음이의어(2개 이상)는 homonymList에 보관
	 */
	private void linkWordsToSentence(Sentence sentence, MorphemeAnalysisResponseDto morphemeResult) {
		List<MorphemeAnalysisResponseDto.Morpheme> morphemes = morphemeResult.getMorphemes();

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

			if (foundWords.size() == 1) {
				// 1개만 있으면 SentenceWord 생성
				Word word = foundWords.get(0);
				SentenceWord sentenceWord = SentenceWord.builder()
						.sentence(sentence)
						.word(word)
						.sequence(i + 1)
						.build();
				sentenceWordRepository.save(sentenceWord);
				log.debug("[Pipeline] SentenceWord 연결 - sentenceId: {}, wordId: {}, wordKr: {}",
						sentence.getSentenceId(), word.getWordId(), wordKr);
			} else {
				// 2개 이상(동음이의어)이면 리스트에 보관
				HomonymWordDto homonymDto = HomonymWordDto.builder()
						.sentence(sentence)
						.sentenceSequence(sentence.getSequence())
						.wordKr(wordKr)
						.homonymWords(foundWords)
						.build();
				homonymList.add(homonymDto);
				log.debug("[Pipeline] 동음이의어 발견 - wordKr: {}, 개수: {}, 문장 시퀀스: {}",
						wordKr, foundWords.size(), sentence.getSequence());
			}
		}
	}

	/**
	 * 동음이의어 리스트 반환 (수동 처리용)
	 */
	public List<HomonymWordDto> getHomonymList() {
		return homonymList;
	}
}
