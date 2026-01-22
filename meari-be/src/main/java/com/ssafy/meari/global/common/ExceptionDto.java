package com.ssafy.meari.global.common;

import com.ssafy.meari.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class ExceptionDto {
//    private HttpStatus status;
    private final String message;

    public ExceptionDto(ErrorCode errorCode) {
        this.message = errorCode.getMessage();
    }

    public static ExceptionDto of(ErrorCode errorCode) {
        return new ExceptionDto(errorCode);
    }
}