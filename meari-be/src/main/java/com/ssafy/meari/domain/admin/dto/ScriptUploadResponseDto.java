package com.ssafy.meari.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스크립트 업로드 결과")
public class ScriptUploadResponseDto {

	@Schema(description = "매칭 결과 CSV 파일 경로", example = "/reports/script_matching_result_20240130_143000.csv")
	private String resultCsvPath;
}
