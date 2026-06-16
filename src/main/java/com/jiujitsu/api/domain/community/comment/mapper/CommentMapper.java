package com.jiujitsu.api.domain.community.comment.mapper;

import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommentMapper {
    /**
     * 댓글 response
     */
    public CommunityCommentsResponse toCommunityCommentsResponse(CommunityComments comments,
                                                                 List<CommunityCommentsResponse> childrenList,
                                                                 Long likeCount,
                                                                 Boolean liked,
                                                                 Boolean isAuthor) {
        Content content = comments.getContent();
        boolean deleted = comments.isDeleted();

        return new CommunityCommentsResponse(
                comments.getId(),
                content.getId(),
                comments.getParentId(),
                deleted ? null : comments.getBody(),
                deleted ? null : likeCount,
                deleted ? null : liked,
                !deleted && isAuthor,
                deleted ? null : CommunityProfileInfo.from(comments.getCreatedBy()),
                comments.getCreatedAt(),
                comments.getUpdatedAt(),
                childrenList,
                deleted
        );
    }

    /**
     * 댓글 response - 좋아요 없는 버전
     */
    public CommunityCommentsResponse toCommunityCommentsResponse(CommunityComments comments,
                                                                 List<CommunityCommentsResponse> childrenList) {
        Content content = comments.getContent();
        boolean deleted = comments.isDeleted();

        return new CommunityCommentsResponse(
                comments.getId(),
                content.getId(),
                comments.getParentId(),
                deleted ? null : comments.getBody(),
                null,
                null,
                deleted ? false : null,
                deleted ? null : CommunityProfileInfo.from(comments.getCreatedBy()),
                comments.getCreatedAt(),
                comments.getUpdatedAt(),
                childrenList,
                deleted
        );
    }
}
