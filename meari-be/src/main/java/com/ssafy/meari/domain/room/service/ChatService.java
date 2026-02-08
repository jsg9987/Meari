package com.ssafy.meari.domain.room.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.meari.domain.room.dto.websocket.ChatMessage;
import com.ssafy.meari.domain.room.entity.Chat;
import com.ssafy.meari.domain.room.repository.ChatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    private static final int MAX_CHAT_COUNT = 100;

    /**
     * 채팅 메시지 저장
     */
    public void saveChat(Long roomId, ChatMessage message) {
        Chat chat = Chat.builder()
                .roomId(roomId)
                .senderId(message.getSenderId())
                .nickname(message.getNickname())
                .message(message.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        chatRepository.save(chat);

        trimOldMessages(roomId);

        log.debug("채팅 메시지 저장 완료: roomId={}, senderId={}", roomId, message.getSenderId());
    }

    /**
     * 방의 채팅 메시지가 MAX_CHAT_COUNT를 초과하면 오래된 순으로 삭제
     */
    private void trimOldMessages(Long roomId) {
        List<Chat> chats = chatRepository.findByRoomIdOrderByTimestampAsc(roomId);

        if (chats.size() > MAX_CHAT_COUNT) {
            int deleteCount = chats.size() - MAX_CHAT_COUNT;
            List<Chat> oldChats = chats.subList(0, deleteCount);
            chatRepository.deleteAll(oldChats);
            log.debug("오래된 채팅 메시지 삭제: roomId={}, 삭제 개수={}", roomId, deleteCount);
        }
    }
}
