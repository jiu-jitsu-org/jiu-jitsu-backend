package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "글쓰기 대회 고르기 후보")
public record CompetitionPickerResponse(
        @Schema(description = "서버 기준 오늘. status·earlyBirdOpen 판단 기준", example = "2026-11-11")
            LocalDate today,
        @Schema(description = "끝나지 않은 대회(마지막 날이 오늘 이후 — 진행 중인 여러 날 대회 포함, 대회일 가까운 순) 다음에 끝난 대회(최근 순). 최대 20개")
            List<CompetitionListItem> competitions
) { }
