package com.ssafy.meari.domain.room.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoomStatus {
    WAITING("대기 중"),
    IN_PROGRESS("학습 중"),
    COMPLETED("종료됨");

    private final String description;
}
