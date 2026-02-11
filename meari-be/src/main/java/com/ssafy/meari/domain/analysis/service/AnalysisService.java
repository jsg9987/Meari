package com.ssafy.meari.domain.analysis.service;

/**
 * 발음 분석 요청 서비스 인터페이스
 * - HTTP 방식과 RabbitMQ 방식 구현체를 전환 가능하도록 설계
 */
public interface AnalysisService {

    /**
     * 멤버별 발음 분석 요청
     *
     * @param roomId 방 ID
     * @param round 라운드 (1 or 2)
     * @param memberId 멤버 ID
     */
    void requestMemberAnalysis(Long roomId, Integer round, Long memberId);
}
