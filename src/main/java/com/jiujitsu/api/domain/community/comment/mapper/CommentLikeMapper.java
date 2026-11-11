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
    public CommentLikeResponse toCommentLikeResponse(CommunityComments comment, CommentLike commentLike, Long likeCount) {
        return toCommentLikeResponse(comment, commentLike != null, likeCount);
    }

    /**
     * 댓글 좋아요 response — 새로 저장한 엔티티 없이 상태만 아는 경우 (동시 요청 충돌, #199)
     */
    public CommentLikeResponse toCommentLikeResponse(CommunityComments comment, boolean isLiked, Long likeCount) {
        return new CommentLikeResponse(
                comment.getId(),
                isLiked,
                likeCount
        );
    }
}
