package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.BeltRank;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionInfo;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRank;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionUniform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "대회 출전 부문 (부문마다 메달 하나)")
public record CompetitionDivisionDto(
        @Schema(description = "수상정보", example = "GOLD") @NotNull
                CompetitionRank competitionRank,
        @Schema(description = "도복/노기 (선택)", example = "GI")
                CompetitionUniform uniform,
        @Schema(description = "출전 띠 (선택)", example = "BLUE")
                BeltRank belt
) {
    public static CompetitionDivisionDto from(CompetitionInfo competitionInfo) {
        return new CompetitionDivisionDto(
                competitionInfo.getCompetitionRank(),
                competitionInfo.getUniform(),
                competitionInfo.getBelt()
        );
    }
}
