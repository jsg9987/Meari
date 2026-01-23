package com.ssafy.meari.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NativeLanguage {
    KR("한국어"),
    VN("베트남어"),
    EN("영어"),
    JP("일본어"),
    CN("중국어"),
    ES("스페인어"),
    FR("프랑스어"),
    DE("독일어"),
    RU("러시아어"),
    AR("아랍어");

    private final String description;
}
