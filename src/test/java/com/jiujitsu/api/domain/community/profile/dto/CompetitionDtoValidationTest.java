package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.CompetitionRank;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionWeightBound;
import com.jiujitsu.api.domain.community.profile.entity.ProfileRequestType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Collections;
import java.util.List;

import static com.jiujitsu.api.domain.community.profile.entity.CompetitionWeightBound.OVER;
import static com.jiujitsu.api.domain.community.profile.entity.CompetitionWeightBound.UNDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대회 기록 요청 검증 — 연도·대회명 길이·부문·메달·체급·앱솔루트·고정·순서.
 */
class CompetitionDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static CompetitionDivisionDto division(Double kg, CompetitionWeightBound bound, Boolean absolute) {
        return new CompetitionDivisionDto(CompetitionRank.GOLD, null, null, kg, bound, absolute);
    }

    private static final List<CompetitionDivisionDto> GOLD = List.of(division(null, null, null));

    private static CompetitionDto competition(Integer year, String name, List<CompetitionDivisionDto> divisions) {
        return new CompetitionDto(year, name, null, null, divisions);
    }

    private boolean isValid(Object dto) {
        return validator.validate(dto).isEmpty();
    }

    @Test
    @DisplayName("연도·메달만 있으면 통과한다 (대회명·도복·띠·체급·고정·순서는 선택)")
    void valid() {
        assertThat(isValid(competition(Year.now().getValue(), "서울 오픈", GOLD))).isTrue();
        assertThat(isValid(competition(1990, "a".repeat(100), GOLD))).isTrue();
        assertThat(isValid(competition(2025, null, GOLD))).isTrue();
        assertThat(isValid(competition(2025, "", GOLD))).isTrue();
    }

    @Test
    @DisplayName("연도가 없거나 1990년 전이거나 올해 이후면 실패한다")
    void invalidYear() {
        assertThat(isValid(competition(null, "서울 오픈", GOLD))).isFalse();
        assertThat(isValid(competition(1989, "서울 오픈", GOLD))).isFalse();
        assertThat(isValid(competition(Year.now().getValue() + 1, "서울 오픈", GOLD))).isFalse();
    }

    @Test
    @DisplayName("대회명이 100자를 넘으면 실패한다")
    void invalidName() {
        assertThat(isValid(competition(2025, "a".repeat(101), GOLD))).isFalse();
    }

    @Test
    @DisplayName("부문이 없거나 부문에 메달이 없으면 실패한다")
    void invalidDivisions() {
        assertThat(isValid(competition(2025, "서울 오픈", null))).isFalse();
        assertThat(isValid(competition(2025, "서울 오픈", List.of()))).isFalse();
        assertThat(isValid(competition(2025, "서울 오픈", Collections.singletonList(null)))).isFalse();
        assertThat(isValid(competition(2025, "서울 오픈",
                List.of(new CompetitionDivisionDto(null, null, null, null, null, null))))).isFalse();
    }

    @Test
    @DisplayName("체급 kg는 0 초과 1000 미만·소수 1자리, 이하/초과와 함께 있어야 한다")
    void weight() {
        assertThat(isValid(division(76.0, UNDER, false))).isTrue();
        assertThat(isValid(division(100.5, OVER, null))).isTrue();
        assertThat(isValid(division(999.9, UNDER, false))).isTrue();

        assertThat(isValid(division(0.0, UNDER, false))).isFalse();
        assertThat(isValid(division(1000.0, UNDER, false))).isFalse();
        assertThat(isValid(division(76.55, UNDER, false))).isFalse();
        assertThat(isValid(division(76.0, null, false))).isFalse();
        assertThat(isValid(division(null, UNDER, false))).isFalse();
    }

    @Test
    @DisplayName("앱솔루트면 체급 kg·이하/초과가 없어야 한다")
    void absolute() {
        assertThat(isValid(division(null, null, true))).isTrue();

        assertThat(isValid(division(76.0, UNDER, true))).isFalse();
    }

    @Test
    @DisplayName("순서는 0 이상, 대회명 없는 기록은 고정할 수 없다")
    void pinAndOrder() {
        assertThat(isValid(new CompetitionDto(2025, "서울 오픈", true, 0, GOLD))).isTrue();
        assertThat(isValid(new CompetitionDto(2025, "", false, null, GOLD))).isTrue();

        assertThat(isValid(new CompetitionDto(2025, "서울 오픈", false, -1, GOLD))).isFalse();
        assertThat(isValid(new CompetitionDto(2025, " ", true, null, GOLD))).isFalse();
        assertThat(isValid(new CompetitionDto(2025, null, true, null, GOLD))).isFalse();
        // 전각 공백(U+3000)만 있는 대회명도 대회명 없음
        assertThat(isValid(new CompetitionDto(2025, "\u3000", true, null, GOLD))).isFalse();
    }

    @Test
    @DisplayName("대회명 유무 판단은 요청(DTO)과 저장 후(엔티티)가 같고, 저장되는 대회명은 앞뒤 공백(전각 포함)을 뺀다")
    void namedConsistency() {
        for (String name : new String[]{"서울 오픈", " 서울 오픈\u3000", "", " ", "\u3000", null}) {
            CompetitionDto dto = new CompetitionDto(2025, name, false, null, GOLD);
            var record = dto.toEntity(null);
            assertThat(record.isNamed()).as("name=[%s]", name).isEqualTo(dto.named());
        }
        assertThat(new CompetitionDto(2025, " 서울 오픈\u3000", false, null, GOLD).toEntity(null).getCompetitionName())
                .isEqualTo("서울 오픈");
        assertThat(new CompetitionDto(2025, "\u3000", false, null, GOLD).toEntity(null).getCompetitionName())
                .isEmpty();
    }

    @Test
    @DisplayName("프로필 요청 안의 대회 목록·부문까지 검증이 이어진다")
    void cascadeFromProfileRequest() {
        CommunityProfileRequest request = new CommunityProfileRequest(ProfileRequestType.COMPETITION,
                null, null, null, null, null,
                List.of(competition(2025, "서울 오픈", List.of(division(76.0, null, false)))),
                null, null, null, null, null, null, null, null, null, null);

        assertThat(isValid(request)).isFalse();
    }
}
