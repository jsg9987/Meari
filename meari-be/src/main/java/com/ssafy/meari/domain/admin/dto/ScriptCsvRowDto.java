package com.ssafy.meari.domain.admin.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CSV 스크립트 행 데이터 DTO")
public class ScriptCsvRowDto {

	@Schema(description = "콘텐츠 ID", example = "1")
	private Long contentId;

	@Schema(description = "시퀀스", example = "1")
	private Integer sequence;

	@Schema(description = "역할 ID", example = "10")
	private Long roleId;

	@Schema(description = "시작 시간(초)", example = "0.000")
	private BigDecimal startTime;

	@Schema(description = "종료 시간(초)", example = "3.500")
	private BigDecimal endTime;

	@Schema(description = "한국어 텍스트", example = "안녕하세요")
	private String textKo;

	@Schema(description = "베트남어 텍스트", example = "Xin chào")
	private String textVn;
}
