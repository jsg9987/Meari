package com.ssafy.meari.domain.mypage.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.mypage.dto.response.MyPageResponseDto;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageServiceImpl implements MyPageService {

	private final MemberRepository memberRepository;

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
}
