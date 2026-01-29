package com.ssafy.meari.domain.admin.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.meari.global.common.ApiResponse;
import com.ssafy.meari.global.pipeline.videosaving.service.ScriptSavingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Admin", description = "관리자 데이터 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDataController {

	private final ScriptSavingService scriptSavingService;

	@Operation(summary = "스크립트 CSV 업로드", description = "CSV 파일을 업로드하여 스크립트를 저장하고 형태소 분석 및 동음이의어 처리를 수행합니다.")
	@PostMapping(value = "/scripts/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Integer> uploadScript(@RequestParam("file") MultipartFile file) {
		log.info("[Admin] 스크립트 CSV 업로드 요청 - 파일명: {}", file.getOriginalFilename());

		int savedCount = scriptSavingService.processCsvFile(file);

		log.info("[Admin] 스크립트 저장 완료 - {}개 문장", savedCount);
		return ApiResponse.success(savedCount);
	}
}
