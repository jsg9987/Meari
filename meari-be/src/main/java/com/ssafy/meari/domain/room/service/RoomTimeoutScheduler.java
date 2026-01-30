package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;
import com.ssafy.meari.domain.room.entity.GamePhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 라운드 녹음 완료 타임아웃 자동 체크 스케줄러
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTimeoutScheduler {

    private final RoomSessionService roomSessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 5초마다 진행 중인 라운드의 타임아웃 체크
     */
    @Scheduled(fixedRate = 5000)
    public void checkRecordingTimeouts() {
        try {
            // Redis에서 모든 방 ID 조회 (room:*:phase 패턴)
            Set<String> phaseKeys = redisTemplate.keys("room:*:phase");
            if (phaseKeys == null || phaseKeys.isEmpty()) {
                return;
            }

            for (String phaseKey : phaseKeys) {
                try {
                    // room:123:phase -> 123 추출
                    String roomIdStr = phaseKey.split(":")[1];
                    Long roomId = Long.parseLong(roomIdStr);

                    GamePhase phase = roomSessionService.getPhase(roomId);
                    if (phase == GamePhase.ROUND_1 || phase == GamePhase.ROUND_2) {
                        Integer round = (phase == GamePhase.ROUND_1) ? 1 : 2;
                        checkAndHandleTimeout(roomId, round, phase);
                    }
                } catch (Exception e) {
                    log.error("라운드 타임아웃 체크 중 오류: phaseKey={}", phaseKey, e);
                }
            }
        } catch (Exception e) {
            log.error("라운드 타임아웃 스케줄러 오류", e);
        }
    }

    /**
     * 특정 라운드의 타임아웃 체크 및 처리
     */
    private void checkAndHandleTimeout(Long roomId, Integer round, GamePhase phase) {
        // 이미 완료 처리되었는지 확인
        if (roomSessionService.isRoundCompleted(roomId, round)) {
            return;
        }

        // 모든 녹음이 완료되었는지 확인 (타임아웃 전 완료된 경우)
        if (roomSessionService.isAllRecordingsComplete(roomId, round)) {
            return;
        }

        // 타임아웃 시간 조회
        Long timeoutMillis = roomSessionService.getRoundTimeout(roomId, round);
        if (timeoutMillis == null) {
            return;
        }

        // 타임아웃 체크
        long currentTime = System.currentTimeMillis();
        if (currentTime > timeoutMillis) {
            log.warn("녹음 완료 타임아웃 자동 처리: roomId={}, round={}", roomId, round);

            // 완료 플래그 설정 (중복 처리 방지)
            roomSessionService.markRoundCompleted(roomId, round);

            // 강제로 완료 처리
            RoomStateMessage completeMessage = RoomStateMessage.recordingsComplete(phase, round);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", completeMessage);

            log.info("녹음 완료 타임아웃 처리 완료: roomId={}, round={}", roomId, round);
        }
    }
}
