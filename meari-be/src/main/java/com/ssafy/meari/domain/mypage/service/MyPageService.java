package com.ssafy.meari.domain.mypage.service;

import com.ssafy.meari.domain.mypage.dto.request.MyPageUpdateRequestDto;
import com.ssafy.meari.domain.mypage.dto.response.MyPageResponseDto;

public interface MyPageService {

	MyPageResponseDto getMyPage(Long memberId);

	void updateMyPage(Long memberId, MyPageUpdateRequestDto request);
}
