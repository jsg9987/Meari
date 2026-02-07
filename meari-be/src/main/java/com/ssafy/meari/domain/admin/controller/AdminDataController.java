package com.ssafy.meari.domain.admin.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.meari.domain.admin.service.CopicImageUploadService;
import com.ssafy.meari.global.common.ApiResponse;
import com.ssafy.meari.global.pipeline.videosaving.service.ScriptSavingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Admin", description = "관리자 데이터 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDataController {

	private final CopicImageUploadService copicImageUploadService;
	private final ScriptSavingService scriptSavingService;

	@Operation(summary = "스크립트 CSV 업로드", description = "CSV 파일을 업로드하여 스크립트를 저장하고 형태소 분석 및 동음이의어 처리를 수행합니다. 매칭 결과는 CSV 파일로 저장됩니다.")
	@PostMapping(value = "/scripts/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<Void>> uploadScript(@RequestParam("file") MultipartFile file) {


		scriptSavingService.processCsvFile(file);
		// log.debug("[Admin] 스크립트 저장 완료 - 결과 CSV: {}", resultCsvPath);

		return ResponseEntity.ok(ApiResponse.successWithoutData());
	}

	@Operation(summary = "코픽 문장별 사진 업로드")
	@PostMapping(value ="/kopic-pictures/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String uploadKopicPicture(@RequestParam MultipartFile file) throws Exception {

		return copicImageUploadService.uploadKopicPicture(file.getBytes(), file.getOriginalFilename());
	}

	@Operation(summary = "코픽 문장별 사진 복수 업로드 _ zip 확장자")
	@PostMapping(value ="/kopic-pictures/upload-multi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<List<String>> uploadMultiKopicPictures(

		@Parameter(
			description = "업로드할 파일들",
			content = @Content(
				array = @ArraySchema(
					schema = @Schema(type = "string", format = "binary")
				)
			)
		)
		@RequestParam("files") MultipartFile zipFile) throws Exception {

		return ResponseEntity.ok(
			copicImageUploadService.uploadMultiKopicPictures(zipFile)
		);
	}
}
