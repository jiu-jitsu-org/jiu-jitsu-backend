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
    public CommunityCommentsResponse toCommunityCommentsResponse(CommunityComments comments, List<CommunityCommentsResponse> childrenList) {
        Content content = comments.getContent();

        return new CommunityCommentsResponse(
                comments.getId(),
                content.getId(),
                comments.getParentId(),
                comments.getBody(),
                CommunityProfileInfo.from(comments.getCreatedBy()),
                comments.getCreatedAt(),
                comments.getUpdatedAt(),
                childrenList
        );
    }
}
