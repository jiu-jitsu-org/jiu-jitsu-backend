package com.jiujitsu.api.domain.community.comment.mapper;

import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileInfo;
import com.jiujitsu.api.global.util.TimeAgoUtil;
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
                                                                 Boolean isAuthor,
                                                                 Boolean isReportedByMe,
                                                                 Boolean isBlocked) {
        Content content = comments.getContent();

        return new CommunityCommentsResponse(
                comments.getId(),
                content.getId(),
                comments.getParentId(),
                isBlocked ? null : comments.getBody(),
                likeCount,
                liked,
                isAuthor,
                comments.isDeleted(),
                comments.getIsReported() || isReportedByMe,
                isBlocked,
                isBlocked ? CommunityProfileInfo.masked() : CommunityProfileInfo.from(comments.getCreatedBy()),
                comments.getCreatedAt(),
                comments.getUpdatedAt(),
                TimeAgoUtil.format(comments.getCreatedAt()),
                childrenList
        );
    }

    /**
     * 댓글 response - 좋아요 없는 버전 (댓글 작성 직후 응답)
     */
    public CommunityCommentsResponse toCommunityCommentsResponse(CommunityComments comments,
                                                                 List<CommunityCommentsResponse> childrenList) {
        Content content = comments.getContent();

        return new CommunityCommentsResponse(
                comments.getId(),
                content.getId(),
                comments.getParentId(),
                comments.getBody(),
                null,
                null,
                null,
                comments.isDeleted(),
                comments.getIsReported(),
                false,
                CommunityProfileInfo.from(comments.getCreatedBy()),
                comments.getCreatedAt(),
                comments.getUpdatedAt(),
                TimeAgoUtil.format(comments.getCreatedAt()),
                childrenList
        );
    }
}
