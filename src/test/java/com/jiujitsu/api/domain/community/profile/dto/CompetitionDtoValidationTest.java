package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.CompetitionRank;
import com.jiujitsu.api.domain.community.profile.entity.ProfileRequestType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대회 기록 요청 검증 — 연도(1990~올해)·대회명(1~100자)·부문 1개 이상·부문별 메달 필수.
 */
class CompetitionDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final List<CompetitionDivisionDto> GOLD = List.of(new CompetitionDivisionDto(CompetitionRank.GOLD, null, null));

    private boolean isValid(CompetitionDto dto) {
        return validator.validate(dto).isEmpty();
    }

    @Test
    @DisplayName("연도·대회명·메달만 있으면 통과한다 (도복·띠는 선택)")
    void valid() {
        assertThat(isValid(new CompetitionDto(Year.now().getValue(), "서울 오픈", GOLD))).isTrue();
        assertThat(isValid(new CompetitionDto(1990, "a".repeat(100), GOLD))).isTrue();
    }

    @Test
    @DisplayName("연도가 없거나 1990년 전이거나 올해 이후면 실패한다")
    void invalidYear() {
        assertThat(isValid(new CompetitionDto(null, "서울 오픈", GOLD))).isFalse();
        assertThat(isValid(new CompetitionDto(1989, "서울 오픈", GOLD))).isFalse();
        assertThat(isValid(new CompetitionDto(Year.now().getValue() + 1, "서울 오픈", GOLD))).isFalse();
    }

    @Test
    @DisplayName("대회명이 비었거나 100자를 넘으면 실패한다")
    void invalidName() {
        assertThat(isValid(new CompetitionDto(2025, null, GOLD))).isFalse();
        assertThat(isValid(new CompetitionDto(2025, "  ", GOLD))).isFalse();
        assertThat(isValid(new CompetitionDto(2025, "a".repeat(101), GOLD))).isFalse();
    }

    @Test
    @DisplayName("부문이 없거나 부문에 메달이 없으면 실패한다")
    void invalidDivisions() {
        assertThat(isValid(new CompetitionDto(2025, "서울 오픈", null))).isFalse();
        assertThat(isValid(new CompetitionDto(2025, "서울 오픈", List.of()))).isFalse();
        assertThat(isValid(new CompetitionDto(2025, "서울 오픈", Collections.singletonList(null)))).isFalse();
        assertThat(isValid(new CompetitionDto(2025, "서울 오픈",
                List.of(new CompetitionDivisionDto(null, null, null))))).isFalse();
    }

    @Test
    @DisplayName("프로필 요청 안의 대회 목록까지 검증이 이어진다")
    void cascadeFromProfileRequest() {
        CommunityProfileRequest request = new CommunityProfileRequest(ProfileRequestType.COMPETITION,
                null, null, null, null, null,
                List.of(new CompetitionDto(2025, "서울 오픈", List.of())),
                null, null, null, null, null, null, null, null, null, null);

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
