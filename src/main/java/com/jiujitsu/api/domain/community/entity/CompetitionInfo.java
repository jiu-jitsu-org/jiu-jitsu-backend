package com.jiujitsu.api.domain.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionInfo {

    @Column(name = "competition_year")
    private Integer competitionYear;    // 대회출전년도

    @Column(name = "competition_month")
    private Integer competitionMonth;   // 대회출전 월

    @Column(name = "competition_name", length = 100)
    private String competitionName; // 대회명

    @Enumerated(EnumType.STRING)
    private CompetitionRank competitionRank;  // 수상정보
}
