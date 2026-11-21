package com.jiujitsu.api.global.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 일반 사용자 계정(USER, OWNER)만 호출 가능한 API에 붙입니다.
 * 어드민 토큰으로 호출하면 403 PERMISSION_DENIED를 반환합니다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAnyRole('USER', 'OWNER')")
public @interface UserOnly {
}
