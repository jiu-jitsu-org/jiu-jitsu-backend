package com.jiujitsu.api.domain.community.comment.factory;

import com.jiujitsu.api.domain.community.comment.entity.CommentLike;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import org.springframework.stereotype.Component;

@Component
public class CommentLikeFactory {
    // entity 생성 클래스

    /**
     * Create Comment Like
     */
    public CommentLike createCommentLike(CommunityComments communityComments) {
        return CommentLike.builder()
                .comment(communityComments)
                .build();
    }
}
