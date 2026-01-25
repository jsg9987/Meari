package com.ssafy.meari.domain.member.service;

import com.ssafy.meari.domain.member.dto.SignupRequestDto;
import com.ssafy.meari.domain.member.dto.response.EmailCheckResponseDto;

public interface MemberService {

	void signup(SignupRequestDto request);

	EmailCheckResponseDto checkEmailExists(String email);
}
