package com.jiujitsu.api.domain.community.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Year;
import java.util.List;

@Schema(description = "대회 기록 (대회 하나에 부문 여러 개)")
public record CompetitionDto(
        @Schema(description = "대회출전 년도", example = "2025") @NotNull @Min(1990L)
                Integer competitionYear,
        @Schema(description = "대회명", example = "서울 오픈") @NotBlank @Size(max = 100)
                String competitionName,
        @Schema(description = "출전 부문") @NotEmpty
                List<@Valid @NotNull CompetitionDivisionDto> divisions
) {
    // 올해 이후 연도는 받지 않는다 (@Max는 상수만 받아서 메서드로 검증)
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "대회출전 년도는 올해 이전이어야 합니다.")
    public boolean isCompetitionYearNotFuture() {
        return competitionYear == null || competitionYear <= Year.now().getValue();
    }

    /**
     * 부문마다 한 줄씩 펼친다
     */
    public List<CompetitionInfo> toEntities() {
        String name = competitionName.trim();
        return divisions.stream()
                .map(division -> CompetitionInfo.builder()
                        .competitionYear(competitionYear)
                        .competitionName(name)
                        .competitionRank(division.competitionRank())
                        .uniform(division.uniform())
                        .belt(division.belt())
                        .build())
                .toList();
    }
}
