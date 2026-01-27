package com.ssafy.meari.global.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ssafy.meari.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {

	private final Member member;

	// 멤버 엔티티에 role 필드가 없고, SecurityConfig에서 role 기반 체크를 하지 않으므로 빈 리스트 설정
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of();
	}

	@Override
	public String getUsername() {
		return member.getEmail(); // 이메일을 아이디로 사용
	}

	@Override
	public String getPassword() {
		return member.getPassword();
	}

	public Member getMember() { // 엔티티를 꺼내 쓰는 메서드
		return member;
	}


}
