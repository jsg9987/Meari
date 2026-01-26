package com.ssafy.meari.domain.room.entity;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room", indexes = {
    @Index(name = "idx_room_owner_id", columnList = "owner_id"),
    @Index(name = "idx_room_content_id", columnList = "content_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "FK_member_TO_room_1"))
    private Member owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_active", nullable = false, length = 20)
    private RoomStatus isActive;

    @Column(name = "password", length = 20)
    private String password;
}
