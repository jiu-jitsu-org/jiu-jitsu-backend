package com.jiujitsu.api.domain.community.dto.comments;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "댓글 목록 조회")
public record CommunityCommentsListResponse(
        @Schema(description = "댓글 아이템 리스트", example = "")
        List<CommunityCommentsItem> commentsItemList
) {
}
