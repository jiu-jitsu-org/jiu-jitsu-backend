package com.jiujitsu.api.domain.community.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "결과 0건 검색어 (관리자)")
public record AdminSearchMissResponse(
        @Schema(description = "검색어 (단어를 소문자 · 공백 하나로 이은 것)", example = "베림보로")
        String keyword,
        @Schema(description = "기간 안에 0건으로 검색된 횟수", example = "7")
        long count,
        @Schema(description = "마지막으로 0건이었던 시각")
        LocalDateTime lastSearchedAt
) { }
