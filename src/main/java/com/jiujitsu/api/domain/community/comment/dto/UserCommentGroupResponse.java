package com.jiujitsu.api.domain.community.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "특정 사용자의 댓글 묶음 (원글 단위)")
public record UserCommentGroupResponse(
        @Schema(description = "원글 요약") BoardSummaryInGroup board,
        @Schema(description = "이 원글에 단 댓글 중 가장 최근 시각") LocalDateTime latestCommentedAt,
        @Schema(description = "이 원글에 단 댓글 목록 (최신순)") List<CommentSummaryInGroup> comments
) {
}
