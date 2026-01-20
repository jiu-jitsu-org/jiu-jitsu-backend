package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.CompetitionInfo;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "대회정보 Dto")
public class CompetitionInfoDto {
    @Schema(description = "대회출전 년도")
    @Min(1990L)
    @Max(2999L)
    private Integer competitionYear;

    @Schema(description = "대회출전 월")
    @Min(1L)
    @Max(12L)
    private Integer competitionMonth;

    @Schema(description = "대회명")
    private String competitionName;

    @Schema(description = "수상정보")
    private CompetitionRank competitionRank;

    public CompetitionInfoDto(CompetitionInfo competitionInfo) {
        this.competitionYear = competitionInfo.getCompetitionYear();
        this.competitionMonth = competitionInfo.getCompetitionMonth();
        this.competitionName = competitionInfo.getCompetitionName();
        this.competitionRank = competitionInfo.getCompetitionRank();
    }

    public CompetitionInfo toEntity() {
        return CompetitionInfo.builder()
                .competitionYear(this.competitionYear)
                .competitionMonth(this.competitionMonth)
                .competitionName(this.competitionName)
                .competitionRank(this.competitionRank)
                .build();
    }
}
