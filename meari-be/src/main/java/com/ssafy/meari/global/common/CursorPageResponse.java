package com.ssafy.meari.global.common;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 무한 스크롤 페이징용 공통 응답 클래스
@Schema(description = "커서 기반 페이지네이션 응답")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CursorPageResponse<T> {

    @Schema(description = "데이터 목록")
    private List<T> contents;

    @Schema(description = "다음 페이지 커서 (다음 페이지 없으면 null)", example = "133")
    private Long nextCursor;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "현재 페이지 데이터 개수", example = "10")
    private int size;

    public static <T> CursorPageResponse<T> of(List<T> contents, Long nextCursor, boolean hasNext) {
        return CursorPageResponse.<T>builder()
                .contents(contents)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(contents.size())
                .build();
    }
}
