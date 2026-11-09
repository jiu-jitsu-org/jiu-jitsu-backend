package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "대회 목록 (기준일 + 양방향 커서)")
public record CompetitionListResponse(
        @Schema(description = "서버 기준 오늘. status·earlyBirdOpen 판단 기준이자 남은 날 계산 기준", example = "2026-11-09")
            LocalDate today,
        @Schema(description = "대회 목록. 항상 대회일 오름차순(같은 날은 id 순)")
            List<CompetitionListItem> competitions,
        @Schema(description = "첫 항목보다 앞(과거) 대회가 있으면 그쪽 커서, 없으면 null", nullable = true)
            String previousCursor,
        @Schema(description = "마지막 항목 뒤 대회가 있으면 그쪽 커서, 없으면 null", nullable = true)
            String nextCursor
) { }
