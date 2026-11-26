package com.jiujitsu.api.domain.community.balance_game.dto;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "관리자 밸런스 게임 목록 요청")
public record AdminBalanceGameListRequest(
        @Schema(description = "gameDate 시작일 (포함)", example = "2026-11-01")
        LocalDate dateFrom,

        @Schema(description = "gameDate 종료일 (포함)", example = "2026-11-30")
        LocalDate dateTo,

        @Schema(description = "상태 필터 (UPCOMING: 시작 전, ONGOING: 진행 중, CLOSED: 마감. 미입력 시 전체)", example = "ONGOING")
        BalanceGameStatus status
) {}
