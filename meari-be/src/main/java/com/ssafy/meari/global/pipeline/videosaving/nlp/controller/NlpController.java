package com.ssafy.meari.global.pipeline.videosaving.nlp.controller;

import com.ssafy.meari.global.pipeline.videosaving.nlp.dto.MorphemeAnalysisResponseDto;
import com.ssafy.meari.global.pipeline.videosaving.nlp.service.NlpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "🛠️ Development", description = "개발/테스트 전용 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/nlp")
@RequiredArgsConstructor
public class NlpController {

    private final NlpService nlpService;

    @Operation(summary = "형태소 분석", description = "OKT를 사용하여 문장의 형태소를 분석합니다.")
    @GetMapping("/morpheme-analysis")
    public ResponseEntity<MorphemeAnalysisResponseDto> analyzeMorphemes(@RequestParam String text) {
        log.info("[Controller] GET /api/v1/nlp/morpheme-analysis - text: {}", text);
        MorphemeAnalysisResponseDto result = nlpService.analyzeMorphemes(text);
        log.info("[Controller] 형태소 분석 응답 - morphemeCount: {}", result.getMorphemes().size());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "배치 형태소 분석", description = "여러 문장의 형태소를 한 번에 분석합니다.")
    @PostMapping("/morpheme-analysis/batch")
    public ResponseEntity<List<MorphemeAnalysisResponseDto>> analyzeMorphemesBatch(@RequestBody List<String> texts) {
        log.info("[Controller] POST /api/v1/nlp/morpheme-analysis/batch - textCount: {}", texts.size());
        List<MorphemeAnalysisResponseDto> results = nlpService.analyzeMorphemesBatch(texts);
        log.info("[Controller] 배치 형태소 분석 응답 - resultCount: {}", results.size());
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "명사 추출", description = "문장에서 명사를 추출합니다.")
    @GetMapping("/extract-nouns")
    public ResponseEntity<List<String>> extractNouns(@RequestParam String text) {
        log.info("[Controller] GET /api/v1/nlp/extract-nouns - text: {}", text);
        List<String> nouns = nlpService.extractNouns(text);
        log.info("[Controller] 명사 추출 응답 - nounCount: {}", nouns.size());
        return ResponseEntity.ok(nouns);
    }

    @Operation(summary = "품사별 단어 추출", description = "문장에서 특정 품사의 단어를 추출합니다.")
    @GetMapping("/extract-by-pos")
    public ResponseEntity<List<String>> extractByPos(
            @RequestParam String text,
            @RequestParam String pos) {
        log.info("[Controller] GET /api/v1/nlp/extract-by-pos - text: {}, pos: {}", text, pos);
        List<String> words = nlpService.extractByPos(text, pos);
        log.info("[Controller] 품사별 단어 추출 응답 - wordCount: {}", words.size());
        return ResponseEntity.ok(words);
    }
}
