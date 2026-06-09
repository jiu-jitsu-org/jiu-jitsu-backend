package com.jiujitsu.api.global.exception.annotation;

import com.jiujitsu.api.global.exception.ErrorCode;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiErrorCodeExamples({ErrorCode.LOGIN_NOT_ACCESS, ErrorCode.USER_NOT_FOUND})
public @interface LoginErrorExamples {
}
