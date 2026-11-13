package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionInfoField;
import io.swagger.v3.oas.annotations.media.Schema;

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
        LocalDateTime createdAt,
        @Schema(description = "처리한 시각 (null = 미처리)", nullable = true)
        LocalDateTime resolvedAt,
        @Schema(description = "처리 메모", nullable = true)
        String resolutionNote
) {
}
