package com.ssafy.meari.global.config;

import com.ssafy.meari.domain.room.service.RoomService;
import com.ssafy.meari.domain.room.service.RoomSessionService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 연결 해제 이벤트 리스너
 * Grace Period 기반 자동 퇴장 처리:
 * - 연결 해제 시 즉시 퇴장하지 않고 Redis에 마킹
 * - Grace Period(30초) 내 재연결하면 마킹 해제
 * - Grace Period 만료 시 자동 퇴장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RoomService roomService;
    private final RoomSessionService roomSessionService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private static final long GRACE_PERIOD_SECONDS = 30;

    /**
     * WebSocket 연결 해제 시 Grace Period 자동 퇴장 처리
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication authentication = (Authentication) accessor.getUser();

        if (authentication == null) {
            return;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long memberId = userDetails.getMember().getMemberId();

        // Redis에서 참여 중인 방 조회
        Long roomId = roomSessionService.getMemberRoom(memberId);
        if (roomId == null) {
            log.info("WebSocket 연결 해제: 참여 중인 방 없음, memberId={}", memberId);
            return;
        }

        log.info("WebSocket 연결 해제 감지: memberId={}, roomId={}, Grace Period {}초 시작", memberId, roomId, GRACE_PERIOD_SECONDS);

        // Grace Period 마킹
        roomSessionService.markDisconnected(roomId, memberId);

        // Grace Period 만료 후 자동 퇴장 스케줄링
        scheduler.schedule(() -> handleGracePeriodExpired(roomId, memberId), GRACE_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Grace Period 만료 시 자동 퇴장 처리
     * 재연결한 경우(disconnected 마킹 해제됨) 퇴장하지 않음
     */
    private void handleGracePeriodExpired(Long roomId, Long memberId) {
        // Grace Period 내 재연결 여부 확인
        if (!roomSessionService.isDisconnected(roomId, memberId)) {
            log.info("Grace Period 내 재연결 완료: roomId={}, memberId={}", roomId, memberId);
            return;
        }

        log.info("Grace Period 만료, 자동 퇴장 실행: roomId={}, memberId={}", roomId, memberId);

        try {
            roomService.leaveRoom(roomId, memberId);
        } catch (BusinessException e) {
            // 이미 퇴장된 경우는 정상으로 처리
            if (e.getErrorCode() == ErrorCode.NOT_FOUND_MEMBER_ROOM) {
                log.debug("자동 퇴장 중단: 이미 퇴장됨, roomId={}, memberId={}", roomId, memberId);
            } else {
                log.error("자동 퇴장 실패: roomId={}, memberId={}", roomId, memberId, e);
            }
        } catch (Exception e) {
            log.error("자동 퇴장 중 예상치 못한 오류: roomId={}, memberId={}", roomId, memberId, e);
        }
    }
}
