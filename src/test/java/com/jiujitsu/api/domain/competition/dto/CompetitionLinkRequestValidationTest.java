package com.jiujitsu.api.domain.competition.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompetitionLinkRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("링크는 http·https 로 시작, 500자까지, 비면 안 됨")
    void validate() {
        assertThat(validator.validate(new CompetitionLinkRequest("https://www.flowcomp.co.kr/championship/53/register"))).isEmpty();
        assertThat(validator.validate(new CompetitionLinkRequest("http://example.com"))).isEmpty();
        assertThat(validator.validate(new CompetitionLinkRequest("javascript:alert(1)"))).hasSize(1);
        assertThat(validator.validate(new CompetitionLinkRequest(" "))).isNotEmpty();
        assertThat(validator.validate(new CompetitionLinkRequest("https://" + "a".repeat(500)))).hasSize(1);
    }
}
