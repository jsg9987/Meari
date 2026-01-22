package com.ssafy.meari.global.common;

import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {
	
	private Boolean success;
	private T data;
	private ExceptionDto error;
	
	// ---- 성공 응답 ---- 
	// successWithData
	public static <T> ApiResponse<T> success(final T data) {
        return new ApiResponse<>(true, data, null);
    }
	
	public static <T> ApiResponse<T> successWithoutData() {
		return new ApiResponse<>(true, null, null);
	}
	
	// ---- 실패 응답 ----
	public static ApiResponse<Object> fail(final HttpMessageNotReadableException e) {
        return new ApiResponse<>(false, null, ExceptionDto.of(ErrorCode.BAD_REQUEST_JSON));
    }

    public static ApiResponse<Object> fail(final HttpMediaTypeNotSupportedException e) {
        return new ApiResponse<>(false, null, ExceptionDto.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    public static ApiResponse<Object> fail(final NoHandlerFoundException e) {
        return new ApiResponse<>(false, null, ExceptionDto.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    public static ApiResponse<Object> fail(final HttpRequestMethodNotSupportedException e) {
        return new ApiResponse<>(false, null, ExceptionDto.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    public static ApiResponse<Object> fail(final MethodArgumentNotValidException e) {
        return new ApiResponse<>(false, null, new ArgumentNotValidExceptionDto(e));
    }

    public static ApiResponse<Object> fail(final ConstraintViolationException e) {
    	
        return new ApiResponse<>(false, null, new ArgumentNotValidExceptionDto(e));
    }

    public static ApiResponse<Object> fail(final MissingPathVariableException e) {
        return new ApiResponse<>(false, null, ExceptionDto.of(ErrorCode.MISSING_PATH_VARIABLE));
    }

    public static ApiResponse<Object> fail(final MissingServletRequestParameterException e) {
        return new ApiResponse<>(false, null, ExceptionDto.of(ErrorCode.MISSING_REQUEST_PARAMETER));
    }

    public static ApiResponse<Object> fail(final MaxUploadSizeExceededException e) {
        return new ApiResponse<>(false, null, ExceptionDto.of(ErrorCode.EXCEEDED_MAX_SIZE));
    }

    public static ApiResponse<Object> fail(final MethodArgumentTypeMismatchException e) {
        return new ApiResponse<>(false, null, ExceptionDto.of(ErrorCode.INVALID_PARAMETER_FORMAT));
    }
    
	public static ApiResponse<Object> fail(final BusinessException e) {
        return new ApiResponse<>(false, null, ExceptionDto.of(e.getErrorCode()));
    }
}
