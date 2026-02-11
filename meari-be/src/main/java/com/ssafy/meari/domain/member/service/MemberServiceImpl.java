package com.ssafy.meari.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.meari.global.auth.request.SignupRequestDto;
import com.ssafy.meari.global.auth.response.EmailCheckResponseDto;
import com.ssafy.meari.domain.member.dto.response.MemberInfoResponseDto;
import com.ssafy.meari.global.auth.response.NicknameCheckResponseDto;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.mapper.MemberMapper;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.global.auth.service.RefreshTokenService;
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
	private final RefreshTokenService refreshTokenService;

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

	@Override
	public EmailCheckResponseDto checkEmailExists(String email) {
		boolean exists = memberRepository.existsByEmail(email);
		return new EmailCheckResponseDto(exists);
	}

	@Override
	public NicknameCheckResponseDto checkNicknameExists(String nickname) {
		boolean exists = memberRepository.existsByNickname(nickname);
		return new NicknameCheckResponseDto(exists);
	}

	@Override
	@Transactional
	public void deleteMember(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

		// Redis에서 RefreshToken 삭제
		refreshTokenService.deleteRefreshToken(member.getEmail());

		memberRepository.delete(member);
	}

	@Override
	public MemberInfoResponseDto getMyInfo(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
		return new MemberInfoResponseDto(
			member.getMemberId(),
			member.getEmail(),
			member.getProfileUrl(),
			member.getNickname()
		);
	}
}
