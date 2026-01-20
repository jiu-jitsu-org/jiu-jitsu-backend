package com.jiujitsu.api.domain.community.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 작성 요청")
public record CommunityCommentsWriteRequest(
        @Schema(description = "게시글 ID", example = "")
        Long postId,
        @Schema(description = "부모 댓글 ID", example = "대댓글 아니라면 NULL")
        Long parentId,
        @Schema(description = "댓글 내용", example = "")
        String body
) {
}
