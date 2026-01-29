package com.ssafy.meari.domain.room.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게임 진행 세부 단계 (Redis 관리)
 *
 * RoomStatus가 IN_PROGRESS일 때 사용되는 세부 진행 상태
 */
@Getter
@RequiredArgsConstructor
public enum GamePhase {

    /**
     * 영상 시청 중
     * - 게임 시작(startGame) 후 자동 설정
     * - 참여자들이 콘텐츠 영상을 시청하는 단계
     */
    WATCHING("영상 시청 중"),

    /**
     * 역할 선택 중
     * - 영상 시청 완료 후 전환
     * - 참여자들이 역할을 선점하는 단계
     */
    ROLE_PICK("역할 선택 중"),

    /**
     * 라운드 1 진행 중
     * - 역할 확정 후 Round1 시작 시 전환
     * - 첫 번째 쉐도잉 라운드
     */
    ROUND_1("라운드 1 진행 중"),

    /**
     * 라운드 2 진행 중
     * - Round1 완료 후 Round2 시작 시 전환
     * - 두 번째 쉐도잉 라운드
     */
    ROUND_2("라운드 2 진행 중");

    private final String description;
}
