package com.ssafy.meari.global.common;

import com.ssafy.meari.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    private Boolean success;
    private T data;

    @Schema(description = "에러 정보 (성공 시 null)", nullable = true, example = "null")
    private ExceptionDto error;

    // ---- 성공 응답 ----
    public static <T> ApiResponse<T> success(final T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> successWithoutData() {
        return new ApiResponse<>(true, null, null);
    }

    // ---- 실패 응답 ----
    public static ApiResponse<Void> fail(final ErrorCode errorCode) {
        return new ApiResponse<>(false, null, ExceptionDto.of(errorCode));
    }

    // 검증 실패는 어떤 필드가 왜 깨졌는지 정보를 응답에 실어야 해서 별도 처리
    public static ApiResponse<Void> fail(final MethodArgumentNotValidException e) {
        return new ApiResponse<>(false, null, new ArgumentNotValidExceptionDto(e));
    }

    public static ApiResponse<Void> fail(final ConstraintViolationException e) {
        return new ApiResponse<>(false, null, new ArgumentNotValidExceptionDto(e));
    }
}
