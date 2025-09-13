package com.jiujitsu.api.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorException extends RuntimeException {
    private final ErrorCode errorCode;

    public ErrorException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
