package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionInfoField;

import java.time.LocalDateTime;

public record AdminCompetitionInfoReportResponse(
        Long reportId,
        Long competitionId,
        String competitionName,
        Integer competitionYear,
        CompetitionInfoField field,
        String detail,
        Long reporterUserId,
        String reporterNickname,
        LocalDateTime createdAt
) {
}
