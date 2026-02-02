package com.jokahobby.api.dto.response;

import com.jokahobby.infra.exception.ErrorCode;

import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetail error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage(), null));
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, Map<String, String> fieldErrors) {
        return new ApiResponse<>(false, null,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage(), fieldErrors));
    }
}
