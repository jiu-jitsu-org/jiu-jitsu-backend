package com.jiujitsu.api.domain.community.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 하나 (알림에서 연 댓글). 대댓글이면 원 댓글과 함께")
public record CommentThreadResponse(
        @Schema(description = "댓글이 달린 게시물의 contentId", example = "10")
        Long contentId,
        @Schema(description = "최상위 댓글 — 댓글 목록 항목과 같은 형식 (대댓글 미리보기 3개 · 대댓글 수 포함). 연 것이 댓글이면 그 댓글, 대댓글이면 원 댓글")
        CommunityCommentsResponse comment,
        @Schema(description = "연 것이 대댓글이면 그 대댓글, 댓글이면 null", nullable = true)
        CommunityCommentsResponse reply
) { }
