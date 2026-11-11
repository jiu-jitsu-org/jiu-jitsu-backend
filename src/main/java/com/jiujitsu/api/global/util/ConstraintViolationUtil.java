package com.jiujitsu.api.global.util;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;

public final class ConstraintViolationUtil {

    private ConstraintViolationUtil() {
    }

    /**
     * 무결성 오류가 이름을 지정한 제약(유니크 등)의 위반인지 확인한다.
     * 제약 이름은 DB 메시지에 담긴다 (PostgreSQL: violates unique constraint "uk_...", H2: 대문자 UK_..._INDEX_x 라 대소문자 무시)
     */
    public static boolean isViolationOf(DataIntegrityViolationException e, String constraintName) {
        String name = constraintName.toLowerCase(Locale.ROOT);
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(name)) {
                return true;
            }
        }
        return false;
    }
}
