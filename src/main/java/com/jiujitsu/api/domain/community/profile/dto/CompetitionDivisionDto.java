package com.jiujitsu.api.domain.community.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiujitsu.api.domain.community.profile.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "대회 출전 부문 (부문마다 메달 하나). 체급과 앱솔루트는 둘 중 하나만")
public record CompetitionDivisionDto(
        @Schema(description = "수상정보", example = "GOLD") @NotNull
                CompetitionRank competitionRank,
        @Schema(description = "도복/노기 (선택)", example = "GI")
                CompetitionUniform competitionUniform,
        @Schema(description = "출전 띠 (선택)", example = "BLUE")
                BeltRank competitionBelt,
        @Digits(integer = 3, fraction = 1)
        @DecimalMin(value = "0.0", inclusive = false)
        @DecimalMax(value = "1000.0", inclusive = false)
        @Schema(description = "체급 kg (선택, 0 초과 1000 미만, 소수 1자리)", example = "76.5")
                Double competitionWeightLimit,
        @Schema(description = "체급 이하/초과 (체급 kg가 있으면 필수, 없으면 null)", example = "UNDER")
                CompetitionWeightBound competitionWeightBound,
        @Schema(description = "앱솔루트 여부 (없으면 false). true면 체급 kg·이하/초과는 null", example = "false")
                Boolean competitionAbsolute
) {
    // 체급 kg와 이하/초과는 같이 있거나 같이 없다
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "체급 kg와 이하/초과는 함께 입력해야 합니다.")
    public boolean isWeightBoundPaired() {
        return (competitionWeightLimit == null) == (competitionWeightBound == null);
    }

    // 앱솔루트는 무제한급이라 체급과 함께 올 수 없다
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "앱솔루트 부문에는 체급을 입력할 수 없습니다.")
    public boolean isAbsoluteWithoutWeight() {
        return !Boolean.TRUE.equals(competitionAbsolute)
                || (competitionWeightLimit == null && competitionWeightBound == null);
    }

    public static CompetitionDivisionDto from(CompetitionDivision division) {
        return new CompetitionDivisionDto(
                division.getCompetitionRank(),
                division.getCompetitionUniform(),
                division.getCompetitionBelt(),
                division.getCompetitionWeightLimit(),
                division.getCompetitionWeightBound(),
                division.isCompetitionAbsolute()
        );
    }

    public CompetitionDivision toEntity(CompetitionRecord record) {
        return CompetitionDivision.builder()
                .competitionRecord(record)
                .competitionRank(competitionRank)
                .competitionUniform(competitionUniform)
                .competitionBelt(competitionBelt)
                .competitionWeightLimit(competitionWeightLimit)
                .competitionWeightBound(competitionWeightBound)
                .competitionAbsolute(Boolean.TRUE.equals(competitionAbsolute))
                .build();
    }
}
