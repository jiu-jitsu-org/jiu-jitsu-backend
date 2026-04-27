package com.jiujitsu.api.domain.community.comment.dto;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileInfo;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityCommentsResponse(
        Long id,
        Long contentId,
        Long parentId,
        String body,
        Long likes,
        Boolean isLiked,
        CommunityProfileInfo author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommunityCommentsResponse> childrenList
) {

}
