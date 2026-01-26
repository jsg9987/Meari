package com.ssafy.meari.domain.mypage.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.mypage.dto.request.MyPageUpdateRequestDto;
import com.ssafy.meari.domain.mypage.dto.request.PasswordCheckRequestDto;
import com.ssafy.meari.domain.mypage.dto.request.PasswordUpdateRequestDto;
import com.ssafy.meari.domain.mypage.dto.response.MyPageResponseDto;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageServiceImpl implements MyPageService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public MyPageResponseDto getMyPage(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

		return new MyPageResponseDto(
			member.getNickname(),
			member.getEmail(),
			member.getProfileUrl(),
			member.getNativeLanguage().getDescription()
		);
	}

	@Override
	@Transactional
	public void updateMyPage(Long memberId, MyPageUpdateRequestDto request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

		// 닉네임 중복 확인 (자기 자신 제외)
		if (memberRepository.existsByNicknameAndMemberIdNot(request.nickname(), memberId)) {
			throw new BusinessException(ErrorCode.DUPLICATED_USER_NICKNAME);
		}

		NativeLanguage nativeLanguage = request.nativeLanguage();
		member.updateProfile(request.nickname(), nativeLanguage);
	}

	@Override
	@Transactional
	public void updatePassword(Long memberId, PasswordUpdateRequestDto request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

		member.updatePassword(passwordEncoder.encode(request.newPassword()));
	}

	@Override
	public void checkPassword(Long memberId, PasswordCheckRequestDto request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
		}
	}
}
