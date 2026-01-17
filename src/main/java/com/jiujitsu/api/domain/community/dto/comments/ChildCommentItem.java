package com.jiujitsu.api.domain.community.dto.comments;

import java.time.LocalDateTime;

public record ChildCommentItem(
        Long id,
        Long postId,
        Long parentId,
        String body,
        CommentAuthor author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
