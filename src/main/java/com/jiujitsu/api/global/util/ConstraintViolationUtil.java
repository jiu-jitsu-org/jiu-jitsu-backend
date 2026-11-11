package com.jiujitsu.api.global.util;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

public final class ConstraintViolationUtil {

    private static final String UNIQUE_VIOLATION = "23505";   // SQL 표준 SQLState (PostgreSQL·H2 공통)

    private ConstraintViolationUtil() {
    }

    /**
     * 무결성 오류가 유니크 제약 위반인지 확인한다 (FK·NOT NULL·CHECK 위반은 false).
     * 제약 이름이 아니라 SQLState 로 본다: DB 에 남은 제약 이름이 엔티티와 다를 수 있다 (예: 이름 없이 만들어진 옛 제약)
     */
    public static boolean isUniqueViolation(DataIntegrityViolationException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
