package com.ssafy.meari.domain.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus {
    PROCESSING("분석 중"),
    COMPLETED("완료"),
    FAILED("실패");

    private final String description;
}
