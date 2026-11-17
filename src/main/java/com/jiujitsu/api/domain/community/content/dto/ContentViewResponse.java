package com.jiujitsu.api.domain.community.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "조회수 올리기('봤음') 응답 — 글 · 밸런스 게임 공통")
public record ContentViewResponse(
        @Schema(description = "컨텐츠 ID", example = "10")
            Long contentId,
        @Schema(description = "올린 뒤 조회수", example = "215")
            long viewCount
) { }
