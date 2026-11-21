package com.jiujitsu.api.domain.log.dto;

import com.jiujitsu.api.global.log.entity.ApiLog;

import java.time.LocalDateTime;

public record ApiLogResponse(
        Long id,
        LocalDateTime requestedAt,
        Boolean success,
        Integer statusCode,
        String errorType,
        String errorMessage,
        Long userId,
        String ipAddress,
        String method,
        String uri
) {
    public static ApiLogResponse from(ApiLog apiLog) {
        return new ApiLogResponse(
                apiLog.getId(),
                apiLog.getRequestedAt(),
                apiLog.getSuccess(),
                apiLog.getStatusCode(),
                apiLog.getErrorType(),
                apiLog.getErrorMessage(),
                apiLog.getUserId(),
                apiLog.getIpAddress(),
                apiLog.getMethod(),
                apiLog.getUri()
        );
    }
}
