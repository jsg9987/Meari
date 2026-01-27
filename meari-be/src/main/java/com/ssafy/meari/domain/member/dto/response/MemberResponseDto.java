package com.ssafy.meari.domain.member.dto.response;

import com.ssafy.meari.domain.member.entity.NativeLanguage;

public record MemberResponseDto(
	Long memberId,
	String email,
	String nickname,
	String profileUrl,
	NativeLanguage nativeLanguage
) {}
