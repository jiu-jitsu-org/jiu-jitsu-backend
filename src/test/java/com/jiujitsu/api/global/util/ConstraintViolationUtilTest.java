package com.jiujitsu.api.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintViolationUtilTest {

    @Test
    @DisplayName("원인 체인 안의 SQLState 23505 는 유니크 위반 — 제약 이름과 관계없음")
    void uniqueViolation() {
        DataIntegrityViolationException e = new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("wrapped", new SQLException("duplicate key value violates unique constraint \"ukk5ced\"", "23505")));

        assertThat(ConstraintViolationUtil.isUniqueViolation(e)).isTrue();
    }

    @Test
    @DisplayName("FK(23503)·NOT NULL(23502) 위반은 유니크 위반이 아니다")
    void otherViolations() {
        assertThat(ConstraintViolationUtil.isUniqueViolation(new DataIntegrityViolationException("fk",
                new SQLException("violates foreign key constraint", "23503")))).isFalse();
        assertThat(ConstraintViolationUtil.isUniqueViolation(new DataIntegrityViolationException("not null",
                new SQLException("null value violates not-null constraint", "23502")))).isFalse();
    }

    @Test
    @DisplayName("SQLException 이 없으면 유니크 위반으로 보지 않는다")
    void noSqlException() {
        assertThat(ConstraintViolationUtil.isUniqueViolation(new DataIntegrityViolationException("unknown"))).isFalse();
    }
}
