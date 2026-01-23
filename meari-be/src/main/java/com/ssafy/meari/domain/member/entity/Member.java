package com.ssafy.meari.domain.member.entity;

import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member", indexes = {
    @Index(name = "idx_member_email", columnList = "email")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Column(name = "profile_url", length = 2048)
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "native_language", nullable = false, length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'KR'")
    private NativeLanguage nativeLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 1)
    private Sex sex;
}
