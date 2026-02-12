package com.ssafy.meari.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ssafy.meari.domain.member.entity.Member;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 이메일로 회원 조회
    Optional<Member> findByEmail(String email);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 닉네임 존재 여부 확인
    boolean existsByNickname(String nickname);

    // 전달받은 memberId의 것을 제외하고, 동일한 닉네임이 존재하는지 확인
    boolean existsByNicknameAndMemberIdNot(String nickname, Long memberId);
}
