package com.ssafy.meari.domain.member.dto.response;

import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.domain.member.entity.Sex;

public record MemberResponseDto(
	Long memberId,
	String email,
	String nickname,
	String profileUrl,
	NativeLanguage nativeLanguage,
	Sex sex
) {}
