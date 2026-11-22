package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionSourceSite;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "대회 출처 정보")
public record CompetitionSourceResponse(
        @Schema(description = "출처 ID") Long id,
        @Schema(description = "출처 사이트") CompetitionSourceSite site,
        @Schema(description = "원문 주소") String sourceUrl,
        @Schema(description = "페이지 제목", nullable = true) String pageTitle,
        @Schema(description = "크롤러 마지막 읽음 시각", nullable = true) LocalDateTime lastCrawledAt,
        @Schema(description = "목록에서 마지막으로 본 날", nullable = true) LocalDate lastSeenInListAt
) {}
