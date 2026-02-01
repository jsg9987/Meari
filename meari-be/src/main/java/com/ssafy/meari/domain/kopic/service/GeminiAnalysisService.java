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

import java.util.Base64;

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
            당신은 한국어 발음 분석 전문가입니다.
            사용자가 한국어 문장을 따라 읽은 음성을 분석하여 정확도와 억양을 평가합니다.

            평가 기준:
            - accuracy (정확도, 0~100): 원문과 발음의 일치도. 음절 누락, 대체, 삽입 등을 고려합니다.
            - intonation (억양, 0~100): 자연스러운 한국어 억양과의 유사도. 높낮이, 강세, 리듬을 고려합니다.

            반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요:
            {
              "accuracy": 0~100 정수,
              "intonation": 0~100 정수,
              "missed_point": "틀리거나 부자연스러운 부분에 대한 구체적인 설명 (한국어)",
              "correction": "올바른 표현 또는 발음 교정 (한국어)",
              "tip": "발음 개선을 위한 실용적인 팁 (한국어)"
            }
            """;

    @Async("geminiAnalysisExecutor")
    @Transactional
    public void analyze(Long kopicReportId, String textKo, byte[] audioData, Long kopicTotalReportId) {
        KopicReport report = kopicReportRepository.findById(kopicReportId)
                .orElse(null);

        if (report == null) {
            log.error("분석 대상 리포트를 찾을 수 없습니다: reportId={}", kopicReportId);
            return;
        }

        try {
            String userMessage = String.format(
                    "원문: \"%s\"\n\n위 음성을 분석하여 JSON 형식으로 응답하세요.",
                    textKo
            );

            String base64Audio = Base64.getEncoder().encodeToString(audioData);
            String requestBody = buildGeminiRequest(userMessage, base64Audio);
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
            int intonation = jsonNode.get("intonation").asInt();

            String detailedAnalysis = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("missed_point", jsonNode.get("missed_point").asText())
                            .put("correction", jsonNode.get("correction").asText())
                            .put("tip", jsonNode.get("tip").asText())
            );

            report.updateAnalysisResult(accuracy, intonation, detailedAnalysis);
            kopicReportRepository.save(report);

            log.debug("Gemini 분석 완료: reportId={}, accuracy={}, intonation={}", kopicReportId, accuracy, intonation);

            kopicAggregationService.tryAggregate(kopicTotalReportId);

        } catch (Exception e) {
            log.error("Gemini 분석 실패: reportId={}, error={}", kopicReportId, e.getMessage(), e);
            report.markAsFailed();
            kopicReportRepository.save(report);
        }
    }

    private String buildGeminiRequest(String userMessage, String base64Audio) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            // system_instruction
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ObjectNode systemPart = objectMapper.createObjectNode();
            systemPart.put("text", SYSTEM_PROMPT);
            systemInstruction.set("parts", objectMapper.createArrayNode().add(systemPart));
            root.set("system_instruction", systemInstruction);

            // contents - text part + audio inlineData part
            ObjectNode content = objectMapper.createObjectNode();
            content.put("role", "user");

            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("text", userMessage);

            ObjectNode audioPart = objectMapper.createObjectNode();
            ObjectNode inlineData = objectMapper.createObjectNode();
            inlineData.put("mimeType", "audio/wav");
            inlineData.put("data", base64Audio);
            audioPart.set("inlineData", inlineData);

            content.set("parts", objectMapper.createArrayNode().add(textPart).add(audioPart));
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
