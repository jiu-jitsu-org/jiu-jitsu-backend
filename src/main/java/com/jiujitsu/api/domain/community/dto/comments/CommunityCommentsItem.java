package com.jiujitsu.api.domain.community.dto.comments;

import com.jiujitsu.api.domain.community.entity.comments.CommunityComments;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityCommentsItem(
        Long id,
        Long postId,
        Long parentId,
        String body,
        CommentAuthor author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ChildCommentItem> childrenList
) {

}
