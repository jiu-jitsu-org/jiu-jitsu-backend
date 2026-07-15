package com.jiujitsu.api.domain.community.balance_game.dto;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "밸런스 게임 선택지")
public record BalanceGameOptionResponse(
        @Schema(description = "선택지 식별자 (투표 요청 시 사용)", example = "A") BalanceGameOption option,
        @Schema(description = "선택지 문구", example = "가드 패스 성공률 100%지만 서브 미션 절대 안 됨") String text,
        @Schema(description = "선택지 이미지 (없으면 null)") ImageInfo image,
        @Schema(description = "득표 수 (퍼센트는 클라이언트에서 계산: round(voteCount/totalVoteCount*100))", example = "7") long voteCount
) { }
