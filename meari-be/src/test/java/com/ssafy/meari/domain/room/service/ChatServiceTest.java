package com.ssafy.meari.domain.room.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.meari.domain.room.dto.websocket.ChatMessage;
import com.ssafy.meari.domain.room.entity.Chat;
import com.ssafy.meari.domain.room.repository.ChatRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 단위 테스트")
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatRepository chatRepository;

    @Nested
    @DisplayName("채팅 메시지 저장")
    class SaveChat {

        @Test
        @DisplayName("성공 - 채팅 메시지 저장 및 올바른 데이터 전달")
        void saveChat_Success() {
            // Given
            Long roomId = 1L;
            ChatMessage message = ChatMessage.builder()
                    .senderId(100L)
                    .nickname("테스터")
                    .message("안녕하세요!")
                    .timestamp(LocalDateTime.now())
                    .build();

            ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
            given(chatRepository.save(chatCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));
            given(chatRepository.findByRoomIdOrderByTimestampAsc(roomId)).willReturn(List.of());

            // When
            chatService.saveChat(roomId, message);

            // Then
            Chat capturedChat = chatCaptor.getValue();
            assertThat(capturedChat.getRoomId()).isEqualTo(roomId);
            assertThat(capturedChat.getSenderId()).isEqualTo(100L);
            assertThat(capturedChat.getNickname()).isEqualTo("테스터");
            assertThat(capturedChat.getMessage()).isEqualTo("안녕하세요!");
            assertThat(capturedChat.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("실패 - Redis 연결 실패 시 예외 전파")
        void saveChat_Fail_RedisException() {
            // Given
            Long roomId = 1L;
            ChatMessage message = ChatMessage.builder()
                    .senderId(100L)
                    .nickname("테스터")
                    .message("안녕하세요!")
                    .build();

            given(chatRepository.save(any(Chat.class))).willThrow(new RuntimeException("Redis 연결 실패"));

            // When & Then
            assertThatThrownBy(() -> chatService.saveChat(roomId, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Redis 연결 실패");
        }
    }

    @Nested
    @DisplayName("오래된 메시지 삭제 (100개 초과 시)")
    class TrimOldMessages {

        @Test
        @DisplayName("성공 - 100개 이하일 때 삭제되지 않음")
        void trimOldMessages_NoDeleteWhenUnderLimit() {
            // Given
            Long roomId = 1L;
            ChatMessage message = ChatMessage.builder()
                    .senderId(100L)
                    .nickname("테스터")
                    .message("테스트")
                    .build();

            List<Chat> existingChats = createChats(roomId, 50);

            given(chatRepository.save(any(Chat.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(chatRepository.findByRoomIdOrderByTimestampAsc(roomId)).willReturn(existingChats);

            // When
            chatService.saveChat(roomId, message);

            // Then
            verify(chatRepository, never()).deleteAll(anyList());
        }

        @Test
        @DisplayName("성공 - 정확히 100개일 때 삭제되지 않음")
        void trimOldMessages_NoDeleteWhenExactlyAtLimit() {
            // Given
            Long roomId = 1L;
            ChatMessage message = ChatMessage.builder()
                    .senderId(100L)
                    .nickname("테스터")
                    .message("테스트")
                    .build();

            List<Chat> existingChats = createChats(roomId, 100);

            given(chatRepository.save(any(Chat.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(chatRepository.findByRoomIdOrderByTimestampAsc(roomId)).willReturn(existingChats);

            // When
            chatService.saveChat(roomId, message);

            // Then
            verify(chatRepository, never()).deleteAll(anyList());
        }

        @Test
        @DisplayName("성공 - 105개일 때 오래된 5개 삭제")
        void trimOldMessages_DeleteOldestWhenOverLimit() {
            // Given
            Long roomId = 1L;
            ChatMessage message = ChatMessage.builder()
                    .senderId(100L)
                    .nickname("테스터")
                    .message("테스트")
                    .build();

            List<Chat> existingChats = createChats(roomId, 105);

            given(chatRepository.save(any(Chat.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(chatRepository.findByRoomIdOrderByTimestampAsc(roomId)).willReturn(existingChats);

            // When
            chatService.saveChat(roomId, message);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Chat>> deleteCaptor = ArgumentCaptor.forClass(List.class);
            verify(chatRepository).deleteAll(deleteCaptor.capture());

            List<Chat> deletedChats = deleteCaptor.getValue();
            assertThat(deletedChats).hasSize(5);
            assertThat(deletedChats.get(0).getMessage()).isEqualTo("메시지 0");
            assertThat(deletedChats.get(4).getMessage()).isEqualTo("메시지 4");
        }

        @Test
        @DisplayName("성공 - 150개일 때 오래된 50개 삭제")
        void trimOldMessages_DeleteManyOldestWhenWayOverLimit() {
            // Given
            Long roomId = 1L;
            ChatMessage message = ChatMessage.builder()
                    .senderId(100L)
                    .nickname("테스터")
                    .message("테스트")
                    .build();

            List<Chat> existingChats = createChats(roomId, 150);

            given(chatRepository.save(any(Chat.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(chatRepository.findByRoomIdOrderByTimestampAsc(roomId)).willReturn(existingChats);

            // When
            chatService.saveChat(roomId, message);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Chat>> deleteCaptor = ArgumentCaptor.forClass(List.class);
            verify(chatRepository).deleteAll(deleteCaptor.capture());

            List<Chat> deletedChats = deleteCaptor.getValue();
            assertThat(deletedChats).hasSize(50);
            assertThat(deletedChats.get(0).getMessage()).isEqualTo("메시지 0");
            assertThat(deletedChats.get(49).getMessage()).isEqualTo("메시지 49");
        }
    }

    private List<Chat> createChats(Long roomId, int count) {
        LocalDateTime now = LocalDateTime.now();
        List<Chat> chats = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chats.add(Chat.builder()
                    .roomId(roomId)
                    .senderId(100L)
                    .nickname("테스터")
                    .message("메시지 " + i)
                    .timestamp(now.plusSeconds(i))
                    .build());
        }
        return chats;
    }
}
