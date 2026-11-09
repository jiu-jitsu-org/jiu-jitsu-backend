package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CompetitionInterestResponse(
        @Schema(description = "대회 ID", example = "1")
        Long competitionId,
        @Schema(description = "처리 후 내 관심 상태", example = "true")
        boolean interested,
        @Schema(description = "처리 후 관심 수 (익명)", example = "12")
        long interestCount
) {
}
