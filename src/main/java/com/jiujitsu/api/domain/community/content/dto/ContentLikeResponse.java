package com.jiujitsu.api.domain.community.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ContentLikeResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long contentID,
        @Schema(description = "좋아요 상태", example = "true")
        Boolean isLiked
) {
}
