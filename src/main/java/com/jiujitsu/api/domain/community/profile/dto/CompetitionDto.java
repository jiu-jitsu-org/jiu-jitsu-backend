package com.jiujitsu.api.domain.community.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.apache.commons.lang3.StringUtils;

import java.time.Year;
import java.util.List;

@Schema(description = "대회 기록 (대회 하나에 부문 여러 개)")
public record CompetitionDto(
        @Schema(description = "대회출전 년도 (1990~올해)", example = "2025") @NotNull @Min(1990L)
                Integer competitionYear,
        @Schema(description = "대회명 (없으면 빈 값 - 나만 보기, 메달 합계·다른 회원 프로필에서 제외)", example = "서울 오픈") @Size(max = 100)
                String competitionName,
        @Schema(description = "대표 기록 고정 여부 (없으면 false, 대회명이 있어야 고정 가능, 최대 3개)", example = "false")
                Boolean competitionPinned,
        @Schema(description = "회원이 정한 순서 (고정한 것끼리, 안 한 것끼리 각각 0이 맨 위. 정한 적 없으면 null)", example = "0") @Min(0L)
                Integer competitionDisplayOrder,
        @Schema(description = "출전 부문") @NotEmpty
                List<@Valid @NotNull CompetitionDivisionDto> divisions
) {
    // 올해 이후 연도는 받지 않는다 (@Max는 상수만 받아서 메서드로 검증)
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "대회출전 년도는 올해까지 입력할 수 있습니다.")
    public boolean isCompetitionYearNotFuture() {
        return competitionYear == null || competitionYear <= Year.now().getValue();
    }

    // 대회명 없는 기록은 나만 보이므로 대표로 고정할 수 없다
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "대회명이 없는 기록은 고정할 수 없습니다.")
    public boolean isNamedIfPinned() {
        return !pinnedOrFalse() || named();
    }

    public boolean pinnedOrFalse() {
        return Boolean.TRUE.equals(competitionPinned);
    }

    public String trimmedName() {
        return StringUtils.trimToEmpty(competitionName);
    }

    // 대회명이 있는 기록인지 - 없으면 나만 보이고 고정·중복 검사·메달 합계에서 빠진다 (저장 후에는 CompetitionRecord.isNamed)
    public boolean named() {
        return !trimmedName().isEmpty();
    }

    public static CompetitionDto from(CompetitionRecord record, List<CompetitionDivisionDto> divisions) {
        return new CompetitionDto(
                record.getCompetitionYear(),
                record.getCompetitionName(),
                record.isCompetitionPinned(),
                record.getCompetitionDisplayOrder(),
                divisions
        );
    }

    public CompetitionRecord toEntity(CommunityProfile profile) {
        CompetitionRecord record = CompetitionRecord.builder()
                .communityProfile(profile)
                .competitionYear(competitionYear)
                .competitionName(trimmedName())
                .competitionPinned(pinnedOrFalse())
                .competitionDisplayOrder(competitionDisplayOrder)
                .build();
        divisions.forEach(division -> record.addDivision(division.toEntity(record)));
        return record;
    }
}
