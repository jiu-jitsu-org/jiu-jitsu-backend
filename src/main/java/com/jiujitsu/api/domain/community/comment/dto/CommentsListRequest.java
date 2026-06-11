package com.jiujitsu.api.domain.community.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 목록 조건")
public record CommentsListRequest(
        @Schema(description = "댓글 ID")
        Long id,
        @Schema(description = "댓글 정렬 조건", example = "CREATE_DESC")
        CommentsSortType sortType
) {
}
