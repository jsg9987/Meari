package com.ssafy.meari.domain.analysis.service;

import com.ssafy.meari.domain.analysis.dto.AnalysisRequestMessage;
import com.ssafy.meari.domain.analysis.dto.SentenceAnalysisInfo;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 분석 요청 데이터 생성 공통 로직
 * HTTP와 RabbitMQ 방식 모두에서 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisRequestBuilder {

    private final ShadowingReportRepository shadowingReportRepository;
    private final SentenceRepository sentenceRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_MEMBER_RECORDINGS = "room:%d:round:%d:member:%d:recordings";

    /**
     * 분석 요청 메시지 생성
     */
    public AnalysisRequestMessage buildRequest(Long roomId, Integer round, Long memberId) {
        log.debug("분석 요청 데이터 생성 시작: roomId={}, round={}, memberId={}", roomId, round, memberId);

        // 1. ShadowingReport 조회
        ShadowingReport report = shadowingReportRepository
                .findByRoom_RoomIdAndRoundAndMember_MemberId(roomId, round, memberId)
                .orElseThrow(() -> {
                    log.warn("ShadowingReport 없음: roomId={}, round={}, memberId={}", roomId, round, memberId);
                    return new BusinessException(ErrorCode.NOT_FOUND_REPORT);
                });

        Long contentId = report.getContent().getContentId();
        Long roleId = report.getRole().getRoleId();

        // 2. Redis에서 녹음 완료된 문장 ID 목록 조회
        String recordingsKey = String.format(KEY_MEMBER_RECORDINGS, roomId, round, memberId);
        Set<String> completedSentenceIds = redisTemplate.opsForSet().members(recordingsKey);

        if (completedSentenceIds == null || completedSentenceIds.isEmpty()) {
            log.warn("완료된 녹음 없음: roomId={}, round={}, memberId={}", roomId, round, memberId);
            throw new BusinessException(ErrorCode.NO_RECORDINGS_FOUND);
        }

        // 3. 문장 정보 조회 및 DTO 생성
        List<SentenceAnalysisInfo> sentences = new ArrayList<>();
        for (String sentenceIdStr : completedSentenceIds) {
            Long sentenceId = Long.parseLong(sentenceIdStr);
            Sentence sentence = sentenceRepository.findById(sentenceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_SENTENCE));

            // Redis에서 실제 S3 URL 조회
            String audioUrlsKey = String.format("room:%d:round:%d:member:%d:audio_urls", roomId, round, memberId);
            Object audioUrlObj = redisTemplate.opsForHash().get(audioUrlsKey, sentenceId.toString());

            if (audioUrlObj == null) {
                log.error("Redis에 audioUrl 없음: roomId={}, round={}, memberId={}, sentenceId={}",
                        roomId, round, memberId, sentenceId);
                throw new BusinessException(ErrorCode.NOT_FOUND_AUDIO_URL);
            }

            String audioUrl = audioUrlObj.toString();

            SentenceAnalysisInfo info = SentenceAnalysisInfo.builder()
                    .sentenceId(sentenceId)
                    .audioUrl(audioUrl)
                    .textKo(sentence.getTextKo())
                    .startTime(sentence.getStartTime().doubleValue())
                    .endTime(sentence.getEndTime().doubleValue())
                    .referenceAudioKey(sentence.getReferenceAudioKey())
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

        log.debug("분석 요청 데이터 생성 완료: sentences={}", sentences.size());
        return message;
    }
}
