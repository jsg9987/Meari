package com.ssafy.meari.domain.room.repository;

import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // 커서 기반 페이징: 특정 테마의 방 목록 조회
    @Query("SELECT r FROM Room r " +
           "JOIN FETCH r.theme t " +
           "JOIN FETCH r.owner o " +
           "WHERE r.status != :completedStatus " +
           "AND (:themeId IS NULL OR t.themeId = :themeId) " +
           "AND (:cursor IS NULL OR r.roomId < :cursor) " +
           "ORDER BY r.roomId DESC")
    List<Room> findRoomsWithCursor(
            @Param("themeId") Long themeId,
            @Param("cursor") Long cursor,
            @Param("completedStatus") RoomStatus completedStatus,
            org.springframework.data.domain.Pageable pageable
    );

    // 전체 방 목록 조회 (COMPLETED 제외)
    @Query("SELECT r FROM Room r " +
           "JOIN FETCH r.theme t " +
           "JOIN FETCH r.owner o " +
           "WHERE r.status != :completedStatus " +
           "AND (:cursor IS NULL OR r.roomId < :cursor) " +
           "ORDER BY r.roomId DESC")
    List<Room> findAllRoomsWithCursor(
            @Param("cursor") Long cursor,
            @Param("completedStatus") RoomStatus completedStatus,
            org.springframework.data.domain.Pageable pageable
    );
}
