package com.jiujitsu.api.domain.community.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "등록된 대회")
public record CompetitionResponse(
        @Schema(description = "대회 ID", example = "1")
            Long competitionId,
        @Schema(description = "대회명", example = "2026 서울 오픈 주짓수 챔피언십")
            String name,
        @Schema(description = "개최 연도 (모르면 null)", example = "2026", nullable = true)
            Integer year
) { }
