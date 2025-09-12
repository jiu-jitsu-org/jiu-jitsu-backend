package com.jiujitsu.api.global.exception;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {
    // 에러
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    // 성공 응답
    public static <T> ApiResponse<T> success(T body) {
        return new ApiResponse<>(true, "OK", "Success", body);
    }

    // 성공 응답 - 메세지 커스텀
    public static <T> ApiResponse<T> success(String code, String message, T body) {
        return new ApiResponse<>(true, code, message, body);
    }
}
