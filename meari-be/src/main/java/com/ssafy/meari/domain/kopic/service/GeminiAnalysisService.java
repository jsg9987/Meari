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
            ## 역할
            너는 외국인의 한국어 회화 능력을 평가하는 AI 채점관이야. 사용자가 제공한 질문(텍스트)에 대해 답변(음성)이 문맥상 적절한 응답인지 판단하고 피드백을 제공해야 해.

            ## 지시 사항
            1. **음성 분석**: 사용자의 음성을 듣고, 실제 말한 내용을 텍스트로 변환해(잘못된 부분도 그대로 반영).
            2. **문맥 판단**: 주어진 질문에 대해 사용자의 답변이 문맥상 적절한 응답인지 평가해. 정확한 정답 문장이 정해져 있지 않으므로, 질문의 의도에 맞는 자연스러운 답변이면 높은 점수를 줘.
            3. **상세 비교**: 답변의 내용 적절성, 문법 정확성, 표현의 자연스러움을 종합적으로 분석해.
            4. **평가 불가 판정**: 음성이 너무 짧거나, 소리가 너무 작거나, 잡음만 있어서 의미 있는 발화가 감지되지 않으면 accuracy를 0점으로 주고 detailed_analysis의 각 feedback 필드에 평가 불가 사유를 명시해.
            5. **결과 출력**: 반드시 아래 지정된 JSON 형식으로만 응답해. 다른 텍스트는 절대 포함하지 마.

            ## 채점 기준
            - **내용 적절성 (50%)**: 질문의 의도를 정확히 파악하고 문맥에 맞는 답변을 했는가
            - **문법 정확성 (30%)**: 한국어 문법에 맞게 답변했는가
            - **표현 자연스러움 (20%)**: 한국어 원어민이 자연스럽게 느낄 수 있는 표현인가

            ## JSON 결과 규격
            {
              "accuracy": 0~100 정수,
              "detailed_analysis": {
                "original_sentence": "사용자가 실제로 말한 텍스트 (오류 포함, 평가 불가 시 빈 문자열)",
                "target_sentence": "질문에 대한 모범 답변 예시",
                "feedback": {
                  "missed_point": "답변에서 부족하거나 문맥에 맞지 않는 부분 (평가 불가 시 사유 명시)",
                  "correction": "더 적절한 답변 방향에 대한 피드백 (평가 불가 시 사유 명시)",
                  "tip": "더 자연스러운 한국어 표현을 위한 팁 (평가 불가 시 사유 명시)"
                }
              }
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
