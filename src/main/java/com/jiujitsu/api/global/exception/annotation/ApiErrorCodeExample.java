package com.jiujitsu.api.global.exception.annotation;

import com.jiujitsu.api.global.exception.ErrorCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodeExample {
    ErrorCode value();
}
