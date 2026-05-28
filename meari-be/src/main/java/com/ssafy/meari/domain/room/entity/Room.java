package com.ssafy.meari.domain.room.entity;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.global.entity.BaseEntity;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room", indexes = {
    @Index(name = "idx_room_owner_id", columnList = "owner_id"),
    @Index(name = "idx_room_theme_id", columnList = "theme_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "FK_member_TO_room_1"))
    private Member owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false, foreignKey = @ForeignKey(name = "FK_theme_TO_room_1"))
    private Theme theme;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "max_people", nullable = false)
    private Integer maxPeople;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoomStatus status;

    @Column(name = "password", length = 20)
    private String password;

    @Builder
    public Room(Member owner, Theme theme, String title, Integer maxPeople, String password) {
        this.owner = owner;
        this.theme = theme;
        this.title = title;
        this.maxPeople = maxPeople;
        this.status = RoomStatus.WAITING;
        this.password = password;
    }

    // 도메인 메서드: 방장 변경
    public void updateOwner(Member newOwner) {
        this.owner = newOwner;
    }

    // 도메인 메서드: 상태 변경
    public void updateStatus(RoomStatus status) {
        this.status = status;
    }

    // 도메인 메서드: 비밀번호 확인
    public boolean hasPassword() {
        return this.password != null && !this.password.isEmpty();
    }

    // 도메인 메서드: 비밀번호 일치 확인
    public boolean isPasswordMatch(String password) {
        if (!hasPassword()) {
            return true;
        }
        return this.password.equals(password);
    }

    // 도메인 메서드: 입장 가능 여부
    public boolean isJoinable() {
        return this.status == RoomStatus.WAITING;
    }

    // 도메인 메서드: 방장 권한 확인 (아니면 예외)
    public void checkOwnerOrThrow(Long memberId) {
        if (!this.owner.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_OWNER);
        }
    }

    // 도메인 메서드: 대기 상태 확인 (아니면 예외)
    public void checkWaitingOrThrow() {
        if (this.status != RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.ROOM_NOT_WAITING);
        }
    }

    // 도메인 메서드: 진행 중 상태 확인 (아니면 예외)
    public void checkInProgressOrThrow() {
        if (this.status != RoomStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.ROOM_NOT_IN_PROGRESS);
        }
    }
}
