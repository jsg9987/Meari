package com.ssafy.meari.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Sex {
    M("남성"),
    F("여성");

    private final String description;
}
