package com.jiujitsu.api.domain.community.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 작성 요청")
public record CommunityCommentsWriteRequest(
        @Schema(description = "게시글 ID", example = "1")
        Long contentId,
        @Schema(description = "부모 댓글 ID (대댓글 아니라면 null 또는 0)", example = "null")
        Long parentId,
        @Schema(description = "댓글 내용", example = "test")
        String body
) {
    // AOS 에서 '부모 없음'을 0으로 보내는 경우를 정규화
    public Long normalizedParentId() {
        return (parentId == null || parentId == 0L) ? null : parentId;
    }
}
