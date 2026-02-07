package com.ssafy.meari.domain.analysis.service;

import com.ssafy.meari.domain.analysis.dto.AnalysisRequestMessage;
import com.ssafy.meari.domain.analysis.dto.AnalysisResultMessage;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP 직접 호출 방식 발음 분석 서비스
 * - Spring Boot → FastAPI 직접 HTTP 호출
 * - RestClient 사용 (Spring Boot 3.2+, WebFlux 의존성 불필요)
 * - @Async로 비동기 처리
 * - DB 업데이트는 AnalysisResultService로 위임 (@Transactional self-invocation 방지)
 */
@Slf4j
@Component("httpAnalysisService")
@RequiredArgsConstructor
public class HttpAnalysisService implements AnalysisService {

    private final RestClient fastApiRestClient;
    private final AnalysisRequestBuilder requestBuilder;
    private final AnalysisResultService analysisResultService;  // 별도 서비스로 분리

    @Value("${analysis.fastapi.timeout:60}")
    private int timeoutSeconds;

    @Async("analysisTaskExecutor")
    @Override
    public void requestMemberAnalysis(Long roomId, Integer round, Long memberId) {
        log.info("[HTTP] 발음 분석 요청 시작: roomId={}, round={}, memberId={}", roomId, round, memberId);

        try {
            // 1. 분석 요청 데이터 생성 (공통 로직)
            AnalysisRequestMessage request = requestBuilder.buildRequest(roomId, round, memberId);

            log.debug("[HTTP] FastAPI 호출 시작: roomId={}, round={}, memberId={}, sentences={}",
                    roomId, round, memberId, request.getSentences().size());

            // 2. FastAPI로 직접 HTTP POST 요청 (RestClient 사용)
            AnalysisResultMessage result = fastApiRestClient
                    .post()
                    .uri("/analyze")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("[HTTP] FastAPI 에러 응답: status={}, body={}",
                                res.getStatusCode(), new String(res.getBody().readAllBytes()));
                        throw new BusinessException(ErrorCode.ANALYSIS_FAILED);
                    })
                    .body(AnalysisResultMessage.class);

            if (result == null) {
                log.error("[HTTP] FastAPI 응답 없음: roomId={}, round={}, memberId={}", roomId, round, memberId);
                throw new BusinessException(ErrorCode.ANALYSIS_FAILED);
            }

            log.info("[HTTP] FastAPI 응답 수신 완료: roomId={}, round={}, memberId={}, accuracy={}, intonation={}",
                    roomId, round, memberId, result.getAccuracy(), result.getIntonation());

            // 3. 결과를 DB에 업데이트
            // 별도 서비스로 위임 (@Transactional self-invocation 방지) -> Spring AOP는 같은 클래스 내 메서드로 구현되어있으면 프록시 작동하지 않음.
            analysisResultService.updateShadowingReport(result);

            log.info("[HTTP] 발음 분석 완료: roomId={}, round={}, memberId={}", roomId, round, memberId);

        } catch (Exception e) {
            log.error("[HTTP] 발음 분석 실패: roomId={}, round={}, memberId={}",
                    roomId, round, memberId, e);
            // 실패해도 예외를 던지지 않음 (비동기 처리)
            // TODO: 실패 시 재시도 로직 추가 고려
        }
    }
}
