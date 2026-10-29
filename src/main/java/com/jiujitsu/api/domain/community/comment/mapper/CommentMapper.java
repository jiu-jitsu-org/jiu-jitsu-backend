package com.jiujitsu.api.domain.community.comment.mapper;

import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileInfo;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.util.TimeAgoUtil;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommentMapper {
    /**
     * 댓글 response
     */
    public CommunityCommentsResponse toCommunityCommentsResponse(CommunityComments comments,
                                                                 CommunityProfile authorProfile,
                                                                 List<CommunityCommentsResponse> childrenList,
                                                                 Long likeCount,
                                                                 Boolean liked,
                                                                 Boolean isAuthor,
                                                                 Boolean isReportedByMe,
                                                                 Boolean isBlocked,
                                                                 Boolean isReplied,
                                                                 Long childCount) {
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
                isReplied,
                isBlocked ? CommunityProfileInfo.masked() : CommunityProfileInfo.from(comments.getCreatedBy(), authorProfile),
                comments.getCreatedAt(),
                comments.getUpdatedAt(),
                TimeAgoUtil.format(comments.getCreatedAt()),
                childCount,
                childrenList
        );
    }

    /**
     * 댓글 response - 좋아요 없는 버전 (댓글 작성 직후 응답)
     * - author: 작성 직후 comments.getCreatedBy() 는 감사(AuditorAware)가 넣은 id 만 있는 User 라 실제 작성자를 받는다
     */
    public CommunityCommentsResponse toCommunityCommentsResponse(CommunityComments comments,
                                                                 User author,
                                                                 CommunityProfile authorProfile,
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
                false,
                CommunityProfileInfo.from(author, authorProfile),
                comments.getCreatedAt(),
                comments.getUpdatedAt(),
                TimeAgoUtil.format(comments.getCreatedAt()),
                0L,
                childrenList
        );
    }
}
