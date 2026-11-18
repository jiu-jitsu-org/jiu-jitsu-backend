package com.jiujitsu.api.domain.community.board.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardCategoryWeeklyTopicRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("써 볼 주제는 100자까지, null 도 통과 (지움) (#267)")
    void maxLength() {
        assertThat(validator.validate(new BoardCategoryWeeklyTopicRequest("가".repeat(100)))).isEmpty();
        assertThat(validator.validate(new BoardCategoryWeeklyTopicRequest(null))).isEmpty();
        assertThat(validator.validate(new BoardCategoryWeeklyTopicRequest("가".repeat(101)))).hasSize(1);
    }
}
