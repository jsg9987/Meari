package com.ssafy.meari.domain.room.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyString;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.ssafy.meari.domain.room.dto.websocket.ChatMessage;
import com.ssafy.meari.domain.room.entity.Chat;
import com.ssafy.meari.domain.room.repository.ChatRepository;
import com.ssafy.meari.domain.room.service.RoomSessionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomWebSocketController 단위 테스트")
class RoomWebSocketControllerTest {

    @InjectMocks
    private RoomWebSocketController roomWebSocketController;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RoomSessionService roomSessionService;

    @Mock
    private ChatRepository chatRepository;

    private Long testRoomId;
    private Long testMemberId;

    @BeforeEach
    void setUp() {
        testRoomId = 1L;
        testMemberId = 100L;
    }

    // chat 핸들러가 RoomWebSocketController에서 주석 처리되어 있어 테스트 비활성화
    // @Nested
    // @DisplayName("채팅 메시지")
    // class ChatMessageTest { ... }

}
