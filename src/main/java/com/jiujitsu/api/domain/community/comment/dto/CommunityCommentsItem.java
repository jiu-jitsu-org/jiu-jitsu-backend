package com.jiujitsu.api.domain.community.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityCommentsItem(
        Long id,
        Long contentId,
        Long parentId,
        String body,
        CommentAuthor author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ChildCommentItem> childrenList
) {

}
