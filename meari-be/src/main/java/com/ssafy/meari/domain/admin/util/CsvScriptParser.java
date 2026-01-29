package com.ssafy.meari.domain.admin.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.meari.domain.admin.dto.ScriptCsvRowDto;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CsvScriptParser {

	private static final String CSV_EXTENSION = "csv";
	private static final Set<String> REQUIRED_HEADERS = Set.of(
		"content_id", "sequence", "role_id", "start_time", "end_time", "text_ko", "text_vn"
	);

	/**
	 * CSV 파일을 파싱하여 ScriptCsvRowDto 리스트로 변환
	 *
	 * @param file MultipartFile 형식의 CSV 파일
	 * @return ScriptCsvRowDto 리스트
	 * @throws BusinessException CSV 파싱 관련 예외
	 */
	public List<ScriptCsvRowDto> parse(MultipartFile file) {
		validateFile(file);

		try (
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
			);
			CSVParser csvParser = CSVFormat.DEFAULT
				.withFirstRecordAsHeader()
				.withIgnoreHeaderCase()
				.withTrim()
				.parse(reader)
		) {
			validateHeader(csvParser.getHeaderMap());

			List<ScriptCsvRowDto> rows = new ArrayList<>();
			int rowNumber = 1; // 헤더는 행 1이므로 데이터는 2부터 시작

			for (CSVRecord record : csvParser) {
				rowNumber++;
				try {
					rows.add(parseRow(record));
				} catch (Exception e) {
					log.error("CSV 파싱 실패 - 파일: {}, 행 번호: {}, 오류: {}",
						file.getOriginalFilename(), rowNumber, e.getMessage());
					throw new BusinessException(ErrorCode.CSV_INVALID_DATA_TYPE);
				}
			}

			if (rows.isEmpty()) {
				throw new BusinessException(ErrorCode.CSV_EMPTY_FILE);
			}

			log.debug("CSV 파싱 완료 - 파일: {}, 행 개수: {}", file.getOriginalFilename(), rows.size());
			return rows;
		} catch (BusinessException e) {
			throw e;
		} catch (IOException e) {
			log.error("CSV 파일 읽기 실패 - 파일: {}, 오류: {}", file.getOriginalFilename(), e.getMessage());
			throw new BusinessException(ErrorCode.CSV_PARSE_ERROR);
		}
	}

	/**
	 * 파일 시스템에서 CSV 파일을 파싱하여 ScriptCsvRowDto 리스트로 변환
	 *
	 * @param filePath CSV 파일의 경로
	 * @return ScriptCsvRowDto 리스트
	 * @throws BusinessException CSV 파싱 관련 예외
	 */
	public List<ScriptCsvRowDto> parseFile(Path filePath) {
		validateFilePath(filePath);

		try (
			BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
			CSVParser csvParser = CSVFormat.DEFAULT
				.withFirstRecordAsHeader()
				.withIgnoreHeaderCase()
				.withTrim()
				.parse(reader)
		) {
			validateHeader(csvParser.getHeaderMap());

			List<ScriptCsvRowDto> rows = new ArrayList<>();
			int rowNumber = 1; // 헤더는 행 1이므로 데이터는 2부터 시작

			for (CSVRecord record : csvParser) {
				rowNumber++;
				try {
					rows.add(parseRow(record));
				} catch (Exception e) {
					log.error("CSV 파싱 실패 - 파일: {}, 행 번호: {}, 오류: {}",
						filePath.getFileName(), rowNumber, e.getMessage());
					throw new BusinessException(ErrorCode.CSV_INVALID_DATA_TYPE);
				}
			}

			if (rows.isEmpty()) {
				throw new BusinessException(ErrorCode.CSV_EMPTY_FILE);
			}

			log.debug("CSV 파싱 완료 - 파일: {}, 행 개수: {}", filePath.getFileName(), rows.size());
			return rows;
		} catch (BusinessException e) {
			throw e;
		} catch (IOException e) {
			log.error("CSV 파일 읽기 실패 - 파일: {}, 오류: {}", filePath.getFileName(), e.getMessage());
			throw new BusinessException(ErrorCode.CSV_PARSE_ERROR);
		}
	}

	/**
	 * MultipartFile 파일 유효성 검증
	 *
	 * @param file 검증할 파일
	 * @throws BusinessException 파일이 유효하지 않은 경우
	 */
	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.CSV_EMPTY_FILE);
		}

		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith("." + CSV_EXTENSION)) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
		}
	}

	/**
	 * 파일 경로 유효성 검증
	 *
	 * @param filePath 검증할 파일 경로
	 * @throws BusinessException 파일이 유효하지 않은 경우
	 */
	private void validateFilePath(Path filePath) {
		if (filePath == null) {
			throw new BusinessException(ErrorCode.CSV_EMPTY_FILE);
		}

		if (!Files.exists(filePath)) {
			log.error("CSV 파일이 존재하지 않음 - 경로: {}", filePath);
			throw new BusinessException(ErrorCode.CSV_PARSE_ERROR);
		}

		if (Files.isDirectory(filePath)) {
			log.error("디렉토리는 처리할 수 없음 - 경로: {}", filePath);
			throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
		}

		String filename = filePath.getFileName().toString().toLowerCase();
		if (!filename.endsWith("." + CSV_EXTENSION)) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
		}
	}

	/**
	 * CSV 헤더 유효성 검증
	 *
	 * @param headerMap CSV 헤더 맵
	 * @throws BusinessException 필수 헤더가 누락된 경우
	 */
	private void validateHeader(java.util.Map<String, Integer> headerMap) {
		Set<String> normalizedHeaders = new HashSet<>();
		for (String header : headerMap.keySet()) {
			normalizedHeaders.add(header.toLowerCase().trim());
		}

		for (String requiredHeader : REQUIRED_HEADERS) {
			if (!normalizedHeaders.contains(requiredHeader)) {
				log.error("CSV 헤더 검증 실패 - 누락된 헤더: {}", requiredHeader);
				throw new BusinessException(ErrorCode.CSV_INVALID_HEADER);
			}
		}
	}

	/**
	 * CSV 행 데이터를 ScriptCsvRowDto로 변환
	 *
	 * @param record CSVRecord 객체
	 * @return 변환된 ScriptCsvRowDto
	 * @throws NumberFormatException, IllegalArgumentException 데이터 타입 변환 실패 시
	 */
	private ScriptCsvRowDto parseRow(CSVRecord record) {
		return ScriptCsvRowDto.builder()
			.contentId(Long.parseLong(record.get("content_id")))
			.sequence(Integer.parseInt(record.get("sequence")))
			.roleId(Long.parseLong(record.get("role_id")))
			.startTime(new BigDecimal(record.get("start_time")))
			.endTime(new BigDecimal(record.get("end_time")))
			.textKo(record.get("text_ko"))
			.textVn(record.get("text_vn"))
			.build();
	}
}
