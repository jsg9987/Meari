package com.ssafy.meari.global.pipeline.videosaving.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.ssafy.meari.domain.admin.util.CsvScriptParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.meari.domain.admin.dto.ScriptCsvRowDto;
import com.ssafy.meari.global.error.exception.BusinessException;

@DisplayName("CsvScriptParser 테스트")
class CsvScriptParserTest {

	private final CsvScriptParser csvScriptParser = new CsvScriptParser();

	@Test
	@DisplayName("정상 CSV 파일 파싱 성공")
	void testParseValidCsvFile() {
		// Given
		String csvContent = "content_id,sequence,role_id,start_time,end_time,text_ko,text_vn\n"
			+ "1,1,10,0.000,3.500,안녕하세요,Xin chào\n"
			+ "1,2,11,3.500,7.200,만나서 반갑습니다,Rất vui được gặp bạn";
		MultipartFile file = new MockMultipartFile(
			"file",
			"script.csv",
			"text/csv",
			csvContent.getBytes()
		);

		// When
		List<ScriptCsvRowDto> result = csvScriptParser.parse(file);

		// Then
		assertThat(result).hasSize(2);

		// 첫 번째 행 검증
		ScriptCsvRowDto firstRow = result.get(0);
		assertThat(firstRow.getContentId()).isEqualTo(1L);
		assertThat(firstRow.getSequence()).isEqualTo(1);
		assertThat(firstRow.getRoleId()).isEqualTo(10L);
		assertThat(firstRow.getStartTime()).isEqualTo(new BigDecimal("0.000"));
		assertThat(firstRow.getEndTime()).isEqualTo(new BigDecimal("3.500"));
		assertThat(firstRow.getTextKo()).isEqualTo("안녕하세요");
		assertThat(firstRow.getTextVn()).isEqualTo("Xin chào");

		// 두 번째 행 검증
		ScriptCsvRowDto secondRow = result.get(1);
		assertThat(secondRow.getContentId()).isEqualTo(1L);
		assertThat(secondRow.getSequence()).isEqualTo(2);
		assertThat(secondRow.getRoleId()).isEqualTo(11L);
		assertThat(secondRow.getStartTime()).isEqualTo(new BigDecimal("3.500"));
		assertThat(secondRow.getEndTime()).isEqualTo(new BigDecimal("7.200"));
		assertThat(secondRow.getTextKo()).isEqualTo("만나서 반갑습니다");
		assertThat(secondRow.getTextVn()).isEqualTo("Rất vui được gặp bạn");
	}

	@Test
	@DisplayName("빈 파일 파싱 실패 - CSV_EMPTY_FILE 예외")
	void testParseEmptyFile() {
		// Given
		MultipartFile file = new MockMultipartFile(
			"file",
			"empty.csv",
			"text/csv",
			new byte[0]
		);

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parse(file))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("CSV_EMPTY_FILE");
			});
	}

	@Test
	@DisplayName("CSV가 아닌 파일 파싱 실패 - UNSUPPORTED_MEDIA_TYPE 예외")
	void testParseNonCsvFile() {
		// Given
		MultipartFile file = new MockMultipartFile(
			"file",
			"script.txt",
			"text/plain",
			"content".getBytes()
		);

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parse(file))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
			});
	}

	@Test
	@DisplayName("헤더 누락 파싱 실패 - CSV_INVALID_HEADER 예외")
	void testParseInvalidHeader() {
		// Given
		String csvContent = "content_id,sequence,role_id,start_time\n"
			+ "1,1,10,0.000";
		MultipartFile file = new MockMultipartFile(
			"file",
			"script.csv",
			"text/csv",
			csvContent.getBytes()
		);

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parse(file))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("CSV_INVALID_HEADER");
			});
	}

	@Test
	@DisplayName("데이터 타입 불일치 파싱 실패 - CSV_INVALID_DATA_TYPE 예외")
	void testParseInvalidDataType() {
		// Given: content_id가 숫자가 아닌 값
		String csvContent = "content_id,sequence,role_id,start_time,end_time,text_ko,text_vn\n"
			+ "invalid,1,10,0.000,3.500,안녕하세요,Xin chào";
		MultipartFile file = new MockMultipartFile(
			"file",
			"script.csv",
			"text/csv",
			csvContent.getBytes()
		);

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parse(file))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("CSV_INVALID_DATA_TYPE");
			});
	}

	@Test
	@DisplayName("대소문자 무시하여 헤더 파싱 성공")
	void testParseHeaderIgnoreCase() {
		// Given
		String csvContent = "CONTENT_ID,SEQUENCE,ROLE_ID,START_TIME,END_TIME,TEXT_KO,TEXT_VN\n"
			+ "1,1,10,0.000,3.500,안녕하세요,Xin chào";
		MultipartFile file = new MockMultipartFile(
			"file",
			"script.csv",
			"text/csv",
			csvContent.getBytes()
		);

		// When
		List<ScriptCsvRowDto> result = csvScriptParser.parse(file);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getContentId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("앞뒤 공백 제거하여 파싱 성공")
	void testParseWithWhitespace() {
		// Given
		String csvContent = "content_id,sequence,role_id,start_time,end_time,text_ko,text_vn\n"
			+ "1,1,10,0.000,3.500,  안녕하세요  ,  Xin chào  ";
		MultipartFile file = new MockMultipartFile(
			"file",
			"script.csv",
			"text/csv",
			csvContent.getBytes()
		);

		// When
		List<ScriptCsvRowDto> result = csvScriptParser.parse(file);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getTextKo()).isEqualTo("안녕하세요");
		assertThat(result.get(0).getTextVn()).isEqualTo("Xin chào");
	}

	@Test
	@DisplayName("헤더만 있고 데이터 없는 파일 파싱 실패 - CSV_EMPTY_FILE 예외")
	void testParseHeaderOnly() {
		// Given
		String csvContent = "content_id,sequence,role_id,start_time,end_time,text_ko,text_vn";
		MultipartFile file = new MockMultipartFile(
			"file",
			"script.csv",
			"text/csv",
			csvContent.getBytes()
		);

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parse(file))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("CSV_EMPTY_FILE");
			});
	}

	@Test
	@DisplayName("sequence 데이터 타입 불일치 파싱 실패")
	void testParseInvalidSequenceDataType() {
		// Given: sequence가 정수가 아닌 값
		String csvContent = "content_id,sequence,role_id,start_time,end_time,text_ko,text_vn\n"
			+ "1,abc,10,0.000,3.500,안녕하세요,Xin chào";
		MultipartFile file = new MockMultipartFile(
			"file",
			"script.csv",
			"text/csv",
			csvContent.getBytes()
		);

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parse(file))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("CSV_INVALID_DATA_TYPE");
			});
	}

	@Test
	@DisplayName("startTime 데이터 타입 불일치 파싱 실패")
	void testParseInvalidStartTimeDataType() {
		// Given: start_time이 숫자가 아닌 값
		String csvContent = "content_id,sequence,role_id,start_time,end_time,text_ko,text_vn\n"
			+ "1,1,10,invalid,3.500,안녕하세요,Xin chào";
		MultipartFile file = new MockMultipartFile(
			"file",
			"script.csv",
			"text/csv",
			csvContent.getBytes()
		);

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parse(file))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("CSV_INVALID_DATA_TYPE");
			});
	}

	// ========== 파일 시스템 I/O 테스트 ==========

	@Test
	@DisplayName("파일 시스템에서 CSV 파일 읽기 성공")
	void testParseFileFromFileSystem() {
		// Given
		Path csvFilePath = Paths.get("src/test/resources/valid_script.csv");

		// When
		List<ScriptCsvRowDto> result = csvScriptParser.parseFile(csvFilePath);

		// Then
		assertThat(result).hasSize(3);

		// 첫 번째 행 검증
		ScriptCsvRowDto firstRow = result.get(0);
		assertThat(firstRow.getContentId()).isEqualTo(1L);
		assertThat(firstRow.getSequence()).isEqualTo(1);
		assertThat(firstRow.getRoleId()).isEqualTo(10L);
		assertThat(firstRow.getStartTime()).isEqualTo(new BigDecimal("0.000"));
		assertThat(firstRow.getEndTime()).isEqualTo(new BigDecimal("3.500"));
		assertThat(firstRow.getTextKo()).isEqualTo("안녕하세요");
		assertThat(firstRow.getTextVn()).isEqualTo("Xin chào");

		// 두 번째 행 검증
		ScriptCsvRowDto secondRow = result.get(1);
		assertThat(secondRow.getSequence()).isEqualTo(2);
		assertThat(secondRow.getRoleId()).isEqualTo(11L);

		// 세 번째 행 검증
		ScriptCsvRowDto thirdRow = result.get(2);
		assertThat(thirdRow.getSequence()).isEqualTo(3);
		assertThat(thirdRow.getTextKo()).isEqualTo("오늘 날씨가 좋네요");
	}

	@Test
	@DisplayName("존재하지 않는 파일 읽기 실패 - CSV_PARSE_ERROR 예외")
	void testParseFileNotFound() {
		// Given
		Path csvFilePath = Paths.get("src/test/resources/nonexistent.csv");

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parseFile(csvFilePath))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("CSV_PARSE_ERROR");
			});
	}

	@Test
	@DisplayName("디렉토리 경로로 읽기 시도 실패 - UNSUPPORTED_MEDIA_TYPE 예외")
	void testParseDirectoryPath() {
		// Given
		Path directoryPath = Paths.get("src/test/resources");

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parseFile(directoryPath))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
			});
	}

	@Test
	@DisplayName("CSV가 아닌 파일 읽기 실패 - UNSUPPORTED_MEDIA_TYPE 예외")
	void testParseNonCsvFileFromFileSystem() {
		// Given
		Path txtFilePath = Paths.get("src/test/resources/valid_script.csv.txt");

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parseFile(txtFilePath))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
			});
	}

	@Test
	@DisplayName("null 경로 읽기 실패 - CSV_EMPTY_FILE 예외")
	void testParseNullPath() {
		// Given
		Path nullPath = null;

		// When & Then
		assertThatThrownBy(() -> csvScriptParser.parseFile(nullPath))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> {
				BusinessException exception = (BusinessException) e;
				assertThat(exception.getErrorCode().name()).isEqualTo("CSV_EMPTY_FILE");
			});
	}
}
