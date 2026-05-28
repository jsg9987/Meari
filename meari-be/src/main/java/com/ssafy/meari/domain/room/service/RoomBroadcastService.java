package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Room 상태 브로드캐스트(STOMP 송신) 전담 서비스.
 *
 * destination·payload는 그대로라 동작은 동일하며, RoomService가 STOMP 송신 세부를 모르게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String ROOM_STATE_TOPIC = "/topic/room/%d/state";

    /**
     * 방 전체 참여자에게 상태 메시지 브로드캐스트
     */
    public void broadcastState(Long roomId, RoomStateMessage message) {
        messagingTemplate.convertAndSend(String.format(ROOM_STATE_TOPIC, roomId), message);
    }
}
