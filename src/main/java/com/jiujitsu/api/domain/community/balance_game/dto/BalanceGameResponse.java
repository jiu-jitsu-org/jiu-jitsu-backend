package com.jiujitsu.api.domain.community.balance_game.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "밸런스 게임 응답")
public record BalanceGameResponse(
        @Schema(description = "컨텐츠 ID (댓글 조회/이동용)", example = "1") Long contentId,
        @Schema(description = "마감 일시 (프론트 카운트다운 기준)") LocalDateTime endAt,
        @Schema(description = "선택지 A") BalanceGameOptionResponse optionA,
        @Schema(description = "선택지 B") BalanceGameOptionResponse optionB,
        @Schema(description = "댓글 수 (0이면 '첫 댓글 남기러 가기'로 노출)", example = "5") long commentCount
) { }
