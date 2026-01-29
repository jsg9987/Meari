package com.ssafy.meari.domain.analysis.service;

import com.ssafy.meari.domain.analysis.dto.AnalysisRequestMessage;
import com.ssafy.meari.domain.analysis.dto.SentenceAnalysisInfo;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.global.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ShadowingReportRepository shadowingReportRepository;
    private final SentenceRepository sentenceRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_MEMBER_RECORDINGS = "room:%d:round:%d:member:%d:recordings";

    /**
     * 특정 멤버의 발음 분석 요청 발행
     * Redis에서 녹음 완료된 문장 정보를 수집하여 RabbitMQ로 전송
     */
    @Transactional(readOnly = true)
    public void requestMemberAnalysis(Long roomId, Integer round, Long memberId) {
        log.debug("멤버 {} 발음 분석 요청 시작 (방: {}, 라운드: {})", memberId, roomId, round);

        // 1. ShadowingReport 조회 (PROCESSING 상태 확인)
        List<ShadowingReport> reports = shadowingReportRepository.findAll().stream()
                .filter(r -> r.getRoom().getRoomId().equals(roomId)
                        && r.getRound().equals(round)
                        && r.getMember().getMemberId().equals(memberId))
                .toList();

        if (reports.isEmpty()) {
            log.warn("ShadowingReport 없음: roomId={}, round={}, memberId={}", roomId, round, memberId);
            return;
        }

        ShadowingReport report = reports.get(0);
        Long contentId = report.getContent().getContentId();
        Long roleId = report.getRole().getRoleId();

        // 2. Redis에서 녹음 완료된 문장 ID 목록 조회
        String recordingsKey = String.format(KEY_MEMBER_RECORDINGS, roomId, round, memberId);
        Set<String> completedSentenceIds = redisTemplate.opsForSet().members(recordingsKey);

        if (completedSentenceIds == null || completedSentenceIds.isEmpty()) {
            log.warn("완료된 녹음 없음: roomId={}, round={}, memberId={}", roomId, round, memberId);
            return;
        }

        // 3. 문장 정보 조회 및 DTO 생성
        List<SentenceAnalysisInfo> sentences = new ArrayList<>();
        for (String sentenceIdStr : completedSentenceIds) {
            Long sentenceId = Long.parseLong(sentenceIdStr);
            Sentence sentence = sentenceRepository.findById(sentenceId)
                    .orElseThrow(() -> new IllegalArgumentException("문장을 찾을 수 없습니다: " + sentenceId));

            // S3 URL 생성 (실제 환경에서는 Redis에서 조회하거나 별도 저장)
            String audioUrl = generateAudioUrl(roomId, round, memberId, sentenceId);

            SentenceAnalysisInfo info = SentenceAnalysisInfo.builder()
                    .sentenceId(sentenceId)
                    .audioUrl(audioUrl)
                    .textKo(sentence.getTextKo())
                    .startTime(sentence.getStartTime().doubleValue())
                    .endTime(sentence.getEndTime().doubleValue())
                    .build();

            sentences.add(info);
        }

        // 4. AnalysisRequestMessage 생성
        AnalysisRequestMessage message = AnalysisRequestMessage.builder()
                .roomId(roomId)
                .round(round)
                .contentId(contentId)
                .memberId(memberId)
                .roleId(roleId)
                .sentences(sentences)
                .build();

        // 5. RabbitMQ로 메시지 발행
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ANALYSIS_EXCHANGE,
                RabbitMQConfig.REQUEST_ROUTING_KEY,
                message
        );

        log.info("발음 분석 요청 발행: roomId={}, round={}, memberId={}, sentences={}",
                roomId, round, memberId, sentences.size());
    }

    /**
     * S3 오디오 URL 생성
     * 실제로는 RecordingCompleteMessage에서 받은 audioUrl을 Redis에 저장해두고 조회해야 함
     */
    private String generateAudioUrl(Long roomId, Integer round, Long memberId, Long sentenceId) {
        // TODO: Redis에서 실제 audioUrl 조회
        // 임시로 패턴 기반 URL 생성
        return String.format("s3://meari-bucket/recordings/room%d/round%d/member%d/sentence%d.wav",
                roomId, round, memberId, sentenceId);
    }
}
