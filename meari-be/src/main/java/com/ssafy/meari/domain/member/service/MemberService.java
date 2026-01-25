package com.ssafy.meari.domain.member.service;

import com.ssafy.meari.domain.member.dto.SignupRequestDto;

public interface MemberService {

	void signup(SignupRequestDto request);
}
