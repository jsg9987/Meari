package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.room.entity.RoomStatus;
import com.ssafy.meari.domain.room.repository.MemberRoomRepository;
import com.ssafy.meari.domain.room.repository.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좀비방 종료 처리 전용 서비스.
 *
 * 기존 RoomService.getRoomList(readOnly)에서 좀비방을 정리하던 코드는 Dirty Checking flush가
 * 일어나지 않아 status 갱신이 누락되는 버그가 있었다. 정리 처리를 REQUIRES_NEW 독립 쓰기
 * 트랜잭션으로 분리해, 조회는 readOnly로 유지하면서 종료 처리는 정상적으로 커밋되게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomCleanupService {

    private final RoomRepository roomRepository;
    private final MemberRoomRepository memberRoomRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeAsCompleted(Long roomId) {
        roomRepository.findById(roomId).ifPresent(room -> {
            if (room.getStatus() != RoomStatus.COMPLETED) {
                room.updateStatus(RoomStatus.COMPLETED);   // 쓰기 트랜잭션이라 Dirty Checking 반영됨
                memberRoomRepository.deleteAllByRoom_RoomId(roomId);
                log.info("좀비방 종료 처리: roomId={}", roomId);
            }
        });
    }
}
