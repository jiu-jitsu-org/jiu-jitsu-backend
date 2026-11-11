package com.jiujitsu.api.domain.community.comment.mapper;

import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import org.springframework.stereotype.Component;

@Component
public class CommentLikeMapper {
    /**
     * 댓글 좋아요 response
     */
    public CommentLikeResponse toCommentLikeResponse(CommunityComments comment, boolean isLiked, Long likeCount) {
        return new CommentLikeResponse(
                comment.getId(),
                isLiked,
                likeCount
        );
    }
}
