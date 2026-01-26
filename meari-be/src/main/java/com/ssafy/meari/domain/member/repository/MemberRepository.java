package com.ssafy.meari.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.meari.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // 전달받은 memberId의 것을 제외하고, 동일한 닉네임이 존재하는지 확인
    boolean existsByNicknameAndMemberIdNot(String nickname, Long memberId);
}
