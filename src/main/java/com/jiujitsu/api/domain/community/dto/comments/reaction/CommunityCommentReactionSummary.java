package com.jiujitsu.api.domain.community.dto.comments.reaction;

import com.jiujitsu.api.domain.community.entity.comments.CommunityCommentReactionType;

public record CommunityCommentReactionSummary(
        long likeCount,
        long dislikeCount,
        CommunityCommentReactionType myType
) {
}
