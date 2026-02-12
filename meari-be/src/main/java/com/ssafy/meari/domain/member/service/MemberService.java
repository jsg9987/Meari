package com.ssafy.meari.domain.member.service;

import com.ssafy.meari.global.auth.request.SignupRequestDto;
import com.ssafy.meari.global.auth.response.EmailCheckResponseDto;
import com.ssafy.meari.domain.member.dto.response.MemberInfoResponseDto;
import com.ssafy.meari.global.auth.response.NicknameCheckResponseDto;

public interface MemberService {

	void signup(SignupRequestDto request);

	EmailCheckResponseDto checkEmailExists(String email);

	NicknameCheckResponseDto checkNicknameExists(String nickname);

	void deleteMember(Long memberId);

	MemberInfoResponseDto getMyInfo(Long memberId);
}
