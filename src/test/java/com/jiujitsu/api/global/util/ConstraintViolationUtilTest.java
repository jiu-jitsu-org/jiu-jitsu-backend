package com.jiujitsu.api.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintViolationUtilTest {

    private static final String NAME = "uk_comment_like_comment_user";

    @Test
    @DisplayName("PostgreSQL 메시지(소문자 제약 이름)를 원인 체인에서 찾는다")
    void postgresMessage() {
        DataIntegrityViolationException e = new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("ERROR: duplicate key value violates unique constraint \"uk_comment_like_comment_user\""));

        assertThat(ConstraintViolationUtil.isViolationOf(e, NAME)).isTrue();
    }

    @Test
    @DisplayName("H2 메시지(대문자 인덱스 이름)도 대소문자 무시로 찾는다")
    void h2Message() {
        DataIntegrityViolationException e = new DataIntegrityViolationException(
                "Unique index or primary key violation: \"PUBLIC.UK_COMMENT_LIKE_COMMENT_USER_INDEX_B ON PUBLIC.COMMENT_LIKE(...)\"");

        assertThat(ConstraintViolationUtil.isViolationOf(e, NAME)).isTrue();
    }

    @Test
    @DisplayName("다른 제약 위반은 아니라고 본다")
    void otherConstraint() {
        DataIntegrityViolationException e = new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("violates foreign key constraint \"fk_comment_like_comment\""));

        assertThat(ConstraintViolationUtil.isViolationOf(e, NAME)).isFalse();
    }
}
