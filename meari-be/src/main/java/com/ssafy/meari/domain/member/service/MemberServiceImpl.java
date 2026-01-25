package com.ssafy.meari.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.meari.domain.member.dto.SignupRequestDto;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.mapper.MemberMapper;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository;
	private final MemberMapper memberMapper;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void signup(SignupRequestDto signupDto) {
		// 1. 이메일, 닉네임 중복 검증
		if (memberRepository.existsByEmail(signupDto.email())) {
			throw new BusinessException(ErrorCode.DUPLICATED_USER);
		}

		if (memberRepository.existsByNickname(signupDto.nickname())) {
			throw new BusinessException(ErrorCode.DUPLICATED_USER_NICKNAME);
		}

		// 2. Member 생성
		Member member = Member.builder()
			.email(signupDto.email())
			.password(passwordEncoder.encode(signupDto.password()))
			.nickname(signupDto.nickname())
			.nativeLanguage(signupDto.nativeLanguage())
			.build();

		// 3. 저장
		memberRepository.save(member);
	}
}
