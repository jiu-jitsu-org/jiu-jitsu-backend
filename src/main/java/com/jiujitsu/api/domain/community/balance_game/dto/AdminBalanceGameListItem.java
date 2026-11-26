package com.jiujitsu.api.domain.community.balance_game.dto;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "관리자 밸런스 게임 목록 아이템")
public record AdminBalanceGameListItem(
        @Schema(description = "밸런스 게임 ID (수정·삭제·통계 조회용)", example = "1") Long id,
        @Schema(description = "컨텐츠 ID", example = "10") Long contentId,
        @Schema(description = "게임 진행 날짜", example = "2026-11-26") LocalDate gameDate,
        @Schema(description = "투표 시작 일시") LocalDateTime startAt,
        @Schema(description = "마감 일시") LocalDateTime endAt,
        @Schema(description = "타이틀 (null이면 미설정)", example = "더 싫은 패배는?", nullable = true) String title,
        @Schema(description = "선택지 A 문구", example = "가드 패스 성공률 100%") String optionAText,
        @Schema(description = "선택지 B 문구", example = "서브 미션 성공률 100%") String optionBText,
        @Schema(description = "전체 득표 수", example = "150") long totalVoteCount,
        @Schema(description = "상태") BalanceGameStatus status
) {}
