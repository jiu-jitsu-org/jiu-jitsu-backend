package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "조회수를 올린 결과")
public record CompetitionViewResponse(
        @Schema(description = "대회 ID", example = "1")
            Long competitionId,
        @Schema(description = "올린 뒤 조회수", example = "215")
            long viewCount
) { }
