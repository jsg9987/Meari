package com.ssafy.meari.domain.room.repository;

import com.ssafy.meari.domain.room.entity.Chat;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends CrudRepository<Chat, String> {
    List<Chat> findByRoomIdOrderByTimestampDesc(Long roomId);
}
