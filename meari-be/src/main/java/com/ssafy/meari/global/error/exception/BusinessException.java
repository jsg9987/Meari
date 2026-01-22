package com.ssafy.meari.global.error.exception;

import com.ssafy.meari.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final ErrorCode errorCode;
	
	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
