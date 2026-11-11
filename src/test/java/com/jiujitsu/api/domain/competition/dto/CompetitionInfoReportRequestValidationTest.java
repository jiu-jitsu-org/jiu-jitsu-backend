package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionInfoField;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompetitionInfoReportRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("항목은 필수, 내용은 200자까지")
    void validate() {
        assertThat(validator.validate(new CompetitionInfoReportRequest(CompetitionInfoField.FEE, null))).isEmpty();
        assertThat(validator.validate(new CompetitionInfoReportRequest(CompetitionInfoField.FEE, "가".repeat(200)))).isEmpty();
        assertThat(validator.validate(new CompetitionInfoReportRequest(CompetitionInfoField.FEE, "가".repeat(201)))).hasSize(1);
        assertThat(validator.validate(new CompetitionInfoReportRequest(null, "내용"))).hasSize(1);
    }
}
