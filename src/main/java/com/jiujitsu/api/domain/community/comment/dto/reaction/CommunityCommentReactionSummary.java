package com.jiujitsu.api.domain.community.comment.dto.reaction;

import com.jiujitsu.api.domain.community.comment.entity.CommunityCommentReactionType;

public record CommunityCommentReactionSummary(
        long likeCount,
        long dislikeCount,
        CommunityCommentReactionType myType
) {
}
