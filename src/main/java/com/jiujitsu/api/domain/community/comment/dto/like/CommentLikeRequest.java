package com.jiujitsu.api.domain.community.comment.dto.like;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommentLikeRequest(
        @Schema(description = "댓글 ID", example = "1")
        Long commentId
) {
}
