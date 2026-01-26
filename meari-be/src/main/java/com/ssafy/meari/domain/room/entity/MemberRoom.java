package com.ssafy.meari.domain.room.entity;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member_room", indexes = {
    @Index(name = "idx_member_room_member_id", columnList = "member_id"),
    @Index(name = "idx_member_room_room_id", columnList = "room_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_room_id")
    private Long memberRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "FK_member_TO_member_room_1"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "FK_room_TO_member_room_1"))
    private Room room;
}
