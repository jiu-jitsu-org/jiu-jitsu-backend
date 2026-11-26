package com.jiujitsu.api.domain.community.balance_game.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "관리자 밸런스 게임 결과 분석")
public record AdminBalanceGameStatsResponse(
        @Schema(description = "밸런스 게임 ID", example = "1") Long id,
        @Schema(description = "컨텐츠 ID", example = "10") Long contentId,
        @Schema(description = "게임 진행 날짜", example = "2026-11-26") LocalDate gameDate,
        @Schema(description = "투표 시작 일시") LocalDateTime startAt,
        @Schema(description = "마감 일시") LocalDateTime endAt,
        @Schema(description = "선택지 A 득표 수", example = "80") long voteCountA,
        @Schema(description = "선택지 B 득표 수", example = "70") long voteCountB,
        @Schema(description = "전체 득표 수", example = "150") long totalVoteCount,
        @Schema(description = "선택지 A 득표율 (0~100)", example = "53.33") double voteRateA,
        @Schema(description = "선택지 B 득표율 (0~100)", example = "46.67") double voteRateB,
        @Schema(description = "댓글 수", example = "25") long commentCount,
        @Schema(description = "조회 수", example = "1200") long viewCount
) {}
