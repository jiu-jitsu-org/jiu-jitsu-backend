package com.jiujitsu.api.domain.community.dto.comments.reaction;

public record CommunityCommentReactionResponse(
        long likeCount,
        long dislikeCount,
        String myReaction
) {
}
