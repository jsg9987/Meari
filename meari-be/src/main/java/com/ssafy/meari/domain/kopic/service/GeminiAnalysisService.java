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
            역할
            - 너는 외국인의 한국어 회화 능력을 평가하는 AI 채점관이야. 사용자가 제공한 질문(텍스트)에 대해 답변(음성)이 문맥상 적절한 응답인지 판단하고 피드백을 제공해야 해.

            지시 사항
            1. STT 원문 고정: 사용자의 음성은 절대 보정·추론·의미 보완하지 말고, 음성 인식 결과(raw STT)를 그대로 텍스트로 변환하라. 발음 오류, 어미 누락, 문장 미완성, 비문도 그대로 유지한다.
            2. 평가 대상 제한: 본 평가는 발음, 억양, 쉐도잉 정확도, 말하기 습관을 평가하지 않는다. 음성 품질이나 발화 방식에 대한 언급은 금지하며, STT 결과 텍스트만을 최종 답변으로 간주한다.
            3. 문맥 중심 평가: 질문에 대해 STT 텍스트가 질문의 의도에 부합하는 의미를 전달하는지 여부만을 평가한다. 표현이 어색하더라도 질문에 대한 핵심 응답이 포함되어 있다면 긍정적으로 평가한다.
            4. 추론 금지: STT 텍스트에 명시적으로 드러나지 않은 의도, 생략된 내용, 추측 가능한 의미를 보완하거나 호의적으로 해석하지 마라. 부족한 내용은 감점 요소로 처리한다.
            5. 평가 불가 판정: STT 결과가 한 단어 수준이거나, 질문과 무관하거나, 의미 있는 응답으로 판단할 수 없는 경우 accuracy를 0점으로 부여하고 모든 feedback 항목에 평가 불가 사유를 명시한다.
            6. 피드백 범위 제한: feedback의 모든 항목은 답변 내용의 충실도와 의미 전달 여부에 대해서만 작성하며, 발음 개선, 말하기 연습, 억양 교정에 대한 조언은 절대 포함하지 마라.
            7. 출력 형식 준수: 반드시 지정된 JSON 형식으로만 응답하며, 그 외의 텍스트는 절대 출력하지 마라.

            채점 기준
            - 내용 적절성 (70%)
            STT 텍스트가 질문의 요구를 충족하는 핵심 의미를 포함하고 있는가.
            문장이 불완전하더라도 질문에 대한 응답 의도가 명확하면 높은 점수를 부여한다.

            - 문법 정확성 (20%)
            STT 결과 기준으로 문법적 오류나 비문 여부를 평가하되, 발음으로 인한 오류는 별도로 추론하거나 보정하지 않는다.

            - 표현 명확성 (10%)
            원어민 수준의 자연스러움이 아닌, 의미 전달의 명확성만을 평가한다.
            발음, 억양, 말의 부드러움은 평가 대상이 아니다.

            JSON 결과 규격
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
            
            - original_sentence: STT로 변환된 텍스트를 그대로 기재한다.
            - target_sentence: 질문에 대한 의미적으로 적절한 답변 예시를 제시하되, 발음이나 말하기 방식과 관련된 표현은 포함하지 않는다.
            - missed_point / correction / tip: 모두 답변 내용의 부족함 또는 의미 전달 관점에서만 작성한다.
            
            평가 불가 처리 (절대 규칙)
            - 어떤 경우에도 반드시 JSON만 반환한다. (설명/마크다운/코드블록 금지)
            - 평가 불가(무응답/소음/의미 불명/문맥 파악 불가)라도 반드시 아래 형식으로 반환한다.
            - 평가 불가 시 accuracy는 0으로 하고, detailed_analysis.feedback 3항목에 사유를 명시한다.
            - original_sentence가 비어도 반드시 JSON을 반환한다.

            평가 불가 예시(JSON)
            {
                "accuracy": 0,
                "detailed_analysis": {
                    "original_sentence": "",
                    "target_sentence": "",
                    "feedback": {
                        "missed_point": "음성이 인식되지 않아 답변을 평가할 수 없습니다.",
                        "correction": "조용한 환경에서 질문에 대한 답변을 다시 말해 주세요.",
                        "tip": "마이크에 가까이 말하고, 문장을 끝까지 또렷하게 발음해 주세요."
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
