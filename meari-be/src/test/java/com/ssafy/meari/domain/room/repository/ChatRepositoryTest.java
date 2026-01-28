package com.ssafy.meari.domain.room.repository;

import com.ssafy.meari.domain.room.entity.Chat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("ChatRepository 통합 테스트")
class ChatRepositoryTest {

    @Autowired
    private ChatRepository chatRepository;

    private Long testRoomId;
    private Long testSenderId;

    @BeforeEach
    void setUp() {
        testRoomId = 1L;
        testSenderId = 100L;
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        chatRepository.deleteAll();
    }

    @Nested
    @DisplayName("채팅 메시지 저장")
    class SaveChat {

        @Test
        @DisplayName("성공 - 채팅 메시지 저장")
        void save_Success() {
            // Given
            Chat chat = Chat.builder()
                    .roomId(testRoomId)
                    .senderId(testSenderId)
                    .nickname("테스터")
                    .message("안녕하세요!")
                    .timestamp(LocalDateTime.now())
                    .build();

            // When
            Chat savedChat = chatRepository.save(chat);

            // Then
            assertThat(savedChat).isNotNull();
            assertThat(savedChat.getId()).isNotNull();
            assertThat(savedChat.getRoomId()).isEqualTo(testRoomId);
            assertThat(savedChat.getSenderId()).isEqualTo(testSenderId);
            assertThat(savedChat.getNickname()).isEqualTo("테스터");
            assertThat(savedChat.getMessage()).isEqualTo("안녕하세요!");
        }

        @Test
        @DisplayName("성공 - 저장된 메시지 조회")
        void save_And_FindById_Success() {
            // Given
            Chat chat = Chat.builder()
                    .roomId(testRoomId)
                    .senderId(testSenderId)
                    .nickname("테스터")
                    .message("테스트 메시지")
                    .timestamp(LocalDateTime.now())
                    .build();

            Chat savedChat = chatRepository.save(chat);

            // When
            Optional<Chat> foundChat = chatRepository.findById(savedChat.getId());

            // Then
            assertThat(foundChat).isPresent();
            assertThat(foundChat.get().getMessage()).isEqualTo("테스트 메시지");
        }
    }

    @Nested
    @DisplayName("방별 채팅 메시지 조회")
    class FindByRoomId {

        @Test
        @DisplayName("성공 - 방별 채팅 메시지 조회 (최신순)")
        void findByRoomIdOrderByTimestampDesc_Success() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            Chat chat1 = Chat.builder()
                    .roomId(testRoomId)
                    .senderId(testSenderId)
                    .nickname("테스터1")
                    .message("첫 번째 메시지")
                    .timestamp(now.minusMinutes(2))
                    .build();

            Chat chat2 = Chat.builder()
                    .roomId(testRoomId)
                    .senderId(testSenderId)
                    .nickname("테스터2")
                    .message("두 번째 메시지")
                    .timestamp(now.minusMinutes(1))
                    .build();

            Chat chat3 = Chat.builder()
                    .roomId(testRoomId)
                    .senderId(testSenderId)
                    .nickname("테스터3")
                    .message("세 번째 메시지")
                    .timestamp(now)
                    .build();

            chatRepository.save(chat1);
            chatRepository.save(chat2);
            chatRepository.save(chat3);

            // When
            List<Chat> chats = chatRepository.findByRoomIdOrderByTimestampDesc(testRoomId);

            // Then
            assertThat(chats).hasSize(3);
            assertThat(chats.get(0).getMessage()).isEqualTo("세 번째 메시지");
            assertThat(chats.get(1).getMessage()).isEqualTo("두 번째 메시지");
            assertThat(chats.get(2).getMessage()).isEqualTo("첫 번째 메시지");
        }

        @Test
        @DisplayName("성공 - 다른 방 메시지는 조회되지 않음")
        void findByRoomIdOrderByTimestampDesc_Success_OnlyTargetRoom() {
            // Given
            Long anotherRoomId = 2L;

            Chat chatRoom1 = Chat.builder()
                    .roomId(testRoomId)
                    .senderId(testSenderId)
                    .nickname("테스터")
                    .message("방1 메시지")
                    .timestamp(LocalDateTime.now())
                    .build();

            Chat chatRoom2 = Chat.builder()
                    .roomId(anotherRoomId)
                    .senderId(testSenderId)
                    .nickname("테스터")
                    .message("방2 메시지")
                    .timestamp(LocalDateTime.now())
                    .build();

            chatRepository.save(chatRoom1);
            chatRepository.save(chatRoom2);

            // When
            List<Chat> chatsRoom1 = chatRepository.findByRoomIdOrderByTimestampDesc(testRoomId);
            List<Chat> chatsRoom2 = chatRepository.findByRoomIdOrderByTimestampDesc(anotherRoomId);

            // Then
            assertThat(chatsRoom1).hasSize(1);
            assertThat(chatsRoom1.get(0).getMessage()).isEqualTo("방1 메시지");

            assertThat(chatsRoom2).hasSize(1);
            assertThat(chatsRoom2.get(0).getMessage()).isEqualTo("방2 메시지");
        }

        @Test
        @DisplayName("성공 - 메시지 없는 방 조회 시 빈 리스트 반환")
        void findByRoomIdOrderByTimestampDesc_Success_EmptyList() {
            // Given
            Long emptyRoomId = 999L;

            // When
            List<Chat> chats = chatRepository.findByRoomIdOrderByTimestampDesc(emptyRoomId);

            // Then
            assertThat(chats).isEmpty();
        }
    }
}
