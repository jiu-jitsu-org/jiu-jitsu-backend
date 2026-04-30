package com.jiujitsu.api.domain.community.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ContentSaveResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long contentID,
        @Schema(description = "저장 상태", example = "true")
        Boolean isSaved
) {
}
