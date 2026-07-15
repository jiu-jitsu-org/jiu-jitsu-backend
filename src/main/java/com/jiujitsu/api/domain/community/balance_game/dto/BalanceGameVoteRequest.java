package com.jiujitsu.api.domain.community.balance_game.dto;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "밸런스 게임 투표 요청")
public record BalanceGameVoteRequest(
        @Schema(description = "선택지 (A/B). 이미 투표한 선택지를 다시 보내면 투표 취소, 다른 선택지면 변경", example = "A")
        @NotNull BalanceGameOption option
) { }
