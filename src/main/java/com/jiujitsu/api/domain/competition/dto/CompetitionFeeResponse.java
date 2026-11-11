package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "참가비 한 단계")
public record CompetitionFeeResponse(
        @Schema(description = "단계 이름", example = "얼리버드")
            String label,
        @Schema(description = "금액 (원)", example = "60000")
            int amount,
        @Schema(description = "이 금액의 마지막 날(당일 포함). null 이면 기한 없음(기본 참가비)", example = "2026-11-08", nullable = true)
            LocalDate until
) { }
