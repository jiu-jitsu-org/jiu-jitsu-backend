package com.jiujitsu.api.domain.community.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "댓글 목록 조건")
public record CommentsListRequest(
        // 이름은 id 지만 댓글을 볼 게시물(게시글 · 밸런스 게임)의 contentId 다 (#258: 없으면 500 → 400)
        @NotNull(message = "게시물 ID는 필수입니다.")
        @Schema(description = "게시물 ID (게시글 · 밸런스 게임의 contentId)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
        @Schema(description = "댓글 정렬 조건", example = "CREATE_DESC")
        CommentsSortType sortType
) {
}
