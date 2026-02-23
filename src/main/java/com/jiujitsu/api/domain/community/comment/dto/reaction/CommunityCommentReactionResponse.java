package com.jiujitsu.api.domain.community.comment.dto.reaction;

public record CommunityCommentReactionResponse(
        long likeCount,
        long dislikeCount,
        String myReaction
) {
}
