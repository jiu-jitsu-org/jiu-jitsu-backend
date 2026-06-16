package com.jiujitsu.api.domain.community.comment.mapper;

import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommentLike;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import org.springframework.stereotype.Component;

@Component
public class CommentLikeMapper {
    /**
     * 댓글 좋아요 response
     */
    public CommentLikeResponse toCommentLikeResponse(CommunityComments comment, CommentLike commentLike) {
        return new CommentLikeResponse(
                comment.getId(),
                commentLike != null
        );
    }
}
