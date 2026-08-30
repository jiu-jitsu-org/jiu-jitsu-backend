package com.jiujitsu.api.domain.community.comment.dto;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityCommentsResponse(
        Long id,
        Long contentId,
        Long parentId,
        String body,
        Long likes,
        Boolean isLiked,
        Boolean isAuthor,
        Boolean isDeleted,
        Boolean isReported,
        Boolean isBlocked,
        CommunityProfileInfo author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        @Schema(description = "등록 경과 시간") String timeAgo,
        List<CommunityCommentsResponse> childrenList
) {

}
