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

    @Builder
    public Member(String email, String password, String nickname, NativeLanguage nativeLanguage) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.nativeLanguage = nativeLanguage != null ? nativeLanguage : NativeLanguage.KR;
    }

    // 도메인 메서드: 닉네임 변경
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 도메인 메서드: 프로필 이미지 변경
    public void updateProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    // 도메인 메서드: 비밀번호 변경
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // 도메인 메서드: 모국어 변경
    public void updateNativeLanguage(NativeLanguage nativeLanguage) {
        this.nativeLanguage = nativeLanguage;
    }
}
