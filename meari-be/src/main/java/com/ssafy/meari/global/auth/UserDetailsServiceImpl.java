package com.ssafy.meari.global.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private final MemberRepository memberRepository;

	// UsernameNotFoundException -> BusinessException
	@Override
	public UserDetails loadUserByUsername(String email) throws BusinessException {
		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

		return new UserDetailsImpl(member);
	}
}
