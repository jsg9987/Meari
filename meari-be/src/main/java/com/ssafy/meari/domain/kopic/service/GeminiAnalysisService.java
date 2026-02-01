package com.ssafy.meari.domain.kopic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.meari.domain.report.entity.KopicReport;
import com.ssafy.meari.domain.report.repository.KopicReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GeminiAnalysisService {

    private final RestTemplate restTemplate;
    private final KopicReportRepository kopicReportRepository;
    private final KopicAggregationService kopicAggregationService;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GeminiAnalysisService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.base-url}") String baseUrl,
            KopicReportRepository kopicReportRepository,
            KopicAggregationService kopicAggregationService,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.kopicReportRepository = kopicReportRepository;
        this.kopicAggregationService = kopicAggregationService;
        this.objectMapper = objectMapper;
    }

    private static final String SYSTEM_PROMPT = """
            ## 역할
            너는 외국인을 위한 전문 한국어 발음 교정 AI 가이드야. 사용자가 제공한 질문(텍스트)과 답변(음성)을 비교하여, 발음의 정확도와 자연스러움을 분석하고 피드백을 제공해야 해.

            ## 지시 사항
            1. **음성 분석**: 사용자의 음성을 듣고, 실제 발음한 그대로를 텍스트로 변환해(잘못 발음한 부분도 그대로 반영).
            2. **문장 유추**: 사용자가 의도했을 '정답 문장'이 무엇인지 문맥상 유추해서 확정해.
            3. **상세 비교**: 문장 단위로 끊어서 분석하고, 특히 발음이 어색하거나 틀린 단어를 찾아내.
            4. **평가 불가 판정**: 음성이 너무 짧거나, 소리가 너무 작거나, 잡음만 있어서 의미 있는 발화가 감지되지 않으면 accuracy를 0점으로 주고 detailed_analysis의 각 feedback 필드에 평가 불가 사유를 명시해.
            5. **결과 출력**: 반드시 아래 지정된 JSON 형식으로만 응답해. 다른 텍스트는 절대 포함하지 마.

            ## JSON 결과 규격
            {
              "accuracy": 0~100 정수,
              "detailed_analysis": {
                "original_sentence": "사용자가 발음한 그대로의 텍스트 (오류 포함, 평가 불가 시 빈 문자열)",
                "target_sentence": "교정된 정답 문장",
                "feedback": {
                  "missed_point": "발음에서 아쉬운 점 (평가 불가 시 사유 명시)",
                  "correction": "어떻게 발음해야 하는지에 대한 피드백 (평가 불가 시 사유 명시)",
                  "tip": "더 자연스럽게 들리기 위한 고급 발음 팁 (평가 불가 시 사유 명시)"
                }
              }
            }
            """;

    @Async("geminiAnalysisExecutor")
    @Transactional
    public void analyze(Long kopicReportId, String textKo, String audioUrl, Long kopicTotalReportId) {
        KopicReport report = kopicReportRepository.findById(kopicReportId)
                .orElse(null);

        if (report == null) {
            log.error("분석 대상 리포트를 찾을 수 없습니다: reportId={}", kopicReportId);
            return;
        }

        try {
            String userMessage = String.format(
                    "원문: \"%s\"\n음성 파일 URL: \"%s\"\n\n위 음성을 분석하여 JSON 형식으로 응답하세요.",
                    textKo, audioUrl
            );

            String requestBody = buildGeminiRequest(userMessage);
            String url = String.format("%s/models/%s:generateContent", baseUrl, model);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            String content = extractContentFromResponse(response.getBody());
            String jsonResponse = extractJson(content);
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            int accuracy = jsonNode.get("accuracy").asInt();

            String detailedAnalysis = objectMapper.writeValueAsString(jsonNode.get("detailed_analysis"));

            report.updateAnalysisResult(accuracy, detailedAnalysis);
            kopicReportRepository.save(report);

            log.debug("Gemini 분석 완료: reportId={}, accuracy={}", kopicReportId, accuracy);

            kopicAggregationService.tryAggregate(kopicTotalReportId);

        } catch (Exception e) {
            log.error("Gemini 분석 실패: reportId={}, error={}", kopicReportId, e.getMessage(), e);
            report.markAsFailed();
            kopicReportRepository.save(report);
        }
    }

    private String buildGeminiRequest(String userMessage) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            // system_instruction
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ObjectNode systemPart = objectMapper.createObjectNode();
            systemPart.put("text", SYSTEM_PROMPT);
            systemInstruction.set("parts", objectMapper.createArrayNode().add(systemPart));
            root.set("system_instruction", systemInstruction);

            // contents
            ObjectNode content = objectMapper.createObjectNode();
            content.put("role", "user");
            ObjectNode userPart = objectMapper.createObjectNode();
            userPart.put("text", userMessage);
            content.set("parts", objectMapper.createArrayNode().add(userPart));
            root.set("contents", objectMapper.createArrayNode().add(content));

            // generationConfig
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("temperature", 0.3);
            generationConfig.put("responseMimeType", "application/json");
            root.set("generationConfig", generationConfig);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Gemini 요청 생성 실패", e);
        }
    }

    private String extractContentFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();
    }

    private String extractJson(String response) {
        String trimmed = response.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }
}
