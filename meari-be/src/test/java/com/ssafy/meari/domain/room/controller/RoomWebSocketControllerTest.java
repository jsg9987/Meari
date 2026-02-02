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

    @Nested
    @DisplayName("채팅 메시지")
    class ChatMessageTest {

        @Test
        @DisplayName("성공 - 채팅 메시지 전송 및 저장")
        void chat_Success() {
            // Given
            ChatMessage message = ChatMessage.builder()
                    .senderId(testMemberId)
                    .nickname("테스터")
                    .message("안녕하세요!")
                    .timestamp(LocalDateTime.now())
                    .build();

            Chat savedChat = Chat.builder()
                    .roomId(testRoomId)
                    .senderId(testMemberId)
                    .nickname("테스터")
                    .message("안녕하세요!")
                    .timestamp(LocalDateTime.now())
                    .build();

            given(chatRepository.save(any(Chat.class))).willReturn(savedChat);
            doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

            // When
            roomWebSocketController.chat(testRoomId, message);

            // Then
            verify(chatRepository).save(any(Chat.class));
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/1/chat"),
                    any(Object.class)
            );
        }

        @Test
        @DisplayName("성공 - 채팅 메시지 저장 시 올바른 데이터 전달")
        void chat_Success_CorrectDataSaved() {
            // Given
            ChatMessage message = ChatMessage.builder()
                    .senderId(testMemberId)
                    .nickname("테스터")
                    .message("테스트 메시지")
                    .timestamp(LocalDateTime.now())
                    .build();

            ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
            Chat savedChat = Chat.builder()
                    .roomId(testRoomId)
                    .senderId(testMemberId)
                    .nickname("테스터")
                    .message("테스트 메시지")
                    .timestamp(LocalDateTime.now())
                    .build();

            when(chatRepository.save(chatCaptor.capture())).thenReturn(savedChat);
            doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

            // When
            roomWebSocketController.chat(testRoomId, message);

            // Then
            Chat capturedChat = chatCaptor.getValue();
            assertThat(capturedChat.getRoomId()).isEqualTo(testRoomId);
            assertThat(capturedChat.getSenderId()).isEqualTo(testMemberId);
            assertThat(capturedChat.getNickname()).isEqualTo("테스터");
            assertThat(capturedChat.getMessage()).isEqualTo("테스트 메시지");
            assertThat(capturedChat.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 저장 실패 시 브로드캐스트 안됨")
        void chat_Fail_SaveException() {
            // Given
            ChatMessage message = ChatMessage.builder()
                    .senderId(testMemberId)
                    .nickname("테스터")
                    .message("안녕하세요!")
                    .build();

            given(chatRepository.save(any(Chat.class))).willThrow(new RuntimeException("Redis 연결 실패"));

            // When
            roomWebSocketController.chat(testRoomId, message);

            // Then
            verify(chatRepository).save(any(Chat.class));
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

}
