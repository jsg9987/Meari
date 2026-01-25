package com.ssafy.meari.domain.member.mapper;

import org.springframework.stereotype.Component;

import com.ssafy.meari.domain.member.dto.response.MemberResponseDto;
import com.ssafy.meari.domain.member.entity.Member;

@Component
public class MemberMapper {

	public MemberResponseDto toResponseDto(Member member) {
		return new MemberResponseDto(
			member.getMemberId(),
			member.getEmail(),
			member.getNickname(),
			member.getProfileUrl(),
			member.getNativeLanguage()
		);
	}
}
