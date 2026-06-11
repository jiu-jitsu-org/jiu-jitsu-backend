package com.jiujitsu.api.global.exception.annotation;

import com.jiujitsu.api.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(responseCode = "200", description = "OK")
@ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER, ErrorCode.SERVER_ERROR})
public @interface CommonApiResponses {
}
