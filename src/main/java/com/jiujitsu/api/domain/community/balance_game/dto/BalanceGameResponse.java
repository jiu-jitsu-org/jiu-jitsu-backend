package com.jiujitsu.api.domain.community.balance_game.dto;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "밸런스 게임 응답")
public record BalanceGameResponse(
        @Schema(description = "컨텐츠 ID (댓글 조회/상세·투표 이동용)", example = "1") Long contentId,
        @Schema(description = "마감 일시 (프론트 카운트다운 기준)") LocalDateTime endAt,
        @Schema(description = "서버 응답 시각 (기기 시계 대신 이 값을 기준으로 남은 시간 계산)") LocalDateTime serverTime,
        @Schema(description = "마감 여부 (true면 결과만 노출, 투표 불가)", example = "false") boolean closed,
        @Schema(description = "선택지 A") BalanceGameOptionResponse optionA,
        @Schema(description = "선택지 B") BalanceGameOptionResponse optionB,
        @Schema(description = "전체 득표 수", example = "10") long totalVoteCount,
        @Schema(description = "내가 투표한 선택지 (null이면 미투표 → 결과·퍼센트 숨김)", example = "A") BalanceGameOption myVote,
        @Schema(description = "댓글 수 (0이면 '첫 댓글 남기러 가기'로 노출)", example = "5") long commentCount
) { }
