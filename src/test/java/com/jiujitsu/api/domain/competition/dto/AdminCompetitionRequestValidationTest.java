package com.jiujitsu.api.domain.competition.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCompetitionRequestValidationTest {

    private static final LocalDate OPEN_DATE = LocalDate.of(2026, 12, 6);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static AdminCompetitionRequest request(String name, Integer year, LocalDate openDate, String place, String noticeUrl,
                                                   List<AdminCompetitionRequest.Fee> fees) {
        return new AdminCompetitionRequest(name, year, openDate, null, null, place, null, null, noticeUrl, null, null,
                null, null, null, fees, null);
    }

    private Set<String> invalidFields(AdminCompetitionRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("대회명·연도·대회일만 있으면 통과")
    void minimal() {
        assertThat(validator.validate(request("서울 오픈", 2026, OPEN_DATE, null, null, null))).isEmpty();
    }

    @Test
    @DisplayName("대회명·연도·대회일은 필수")
    void required() {
        assertThat(invalidFields(request(" ", null, null, null, null, null)))
                .containsExactlyInAnyOrder("name", "competitionYear", "openDate");
    }

    @Test
    @DisplayName("글자 칸은 컬럼 길이까지 (장소 200자)")
    void length() {
        assertThat(validator.validate(request("서울 오픈", 2026, OPEN_DATE, "가".repeat(200), null, null))).isEmpty();
        assertThat(invalidFields(request("서울 오픈", 2026, OPEN_DATE, "가".repeat(201), null, null))).containsExactly("place");
    }

    @Test
    @DisplayName("대회 공지 링크는 http·https 로 시작")
    void noticeUrl() {
        assertThat(validator.validate(request("서울 오픈", 2026, OPEN_DATE, null, "https://example.com", null))).isEmpty();
        assertThat(validator.validate(request("서울 오픈", 2026, OPEN_DATE, null, "http://example.com", null))).isEmpty();
        assertThat(invalidFields(request("서울 오픈", 2026, OPEN_DATE, null, "javascript:alert(1)", null))).containsExactly("noticeUrl");
        assertThat(invalidFields(request("서울 오픈", 2026, OPEN_DATE, null, "example.com", null))).containsExactly("noticeUrl");
    }

    @Test
    @DisplayName("참가비 단계: 이름 필수·20자까지, 금액 필수·0 이상, 단계 안의 null 은 거절")
    void fees() {
        assertThat(validator.validate(request("서울 오픈", 2026, OPEN_DATE, null, null,
                List.of(new AdminCompetitionRequest.Fee("무료", 0, null))))).isEmpty();
        assertThat(invalidFields(request("서울 오픈", 2026, OPEN_DATE, null, null,
                List.of(new AdminCompetitionRequest.Fee("", -1, null))))).containsExactlyInAnyOrder("fees[0].label", "fees[0].amount");
        assertThat(invalidFields(request("서울 오픈", 2026, OPEN_DATE, null, null,
                List.of(new AdminCompetitionRequest.Fee("가".repeat(21), null, null))))).containsExactlyInAnyOrder("fees[0].label", "fees[0].amount");
        assertThat(validator.validate(request("서울 오픈", 2026, OPEN_DATE, null, null,
                Arrays.asList((AdminCompetitionRequest.Fee) null)))).hasSize(1);
    }
}
