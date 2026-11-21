package com.jiujitsu.api.domain.community.comment.dto;

import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.global.util.TimeAgoUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "댓글 묶음 내 댓글 요소")
public record CommentSummaryInGroup(
        @Schema(description = "댓글 ID") Long id,
        @Schema(description = "부모 댓글 ID (최상위 댓글은 null)") Long parentId,
        @Schema(description = "댓글 내용") String body,
        @Schema(description = "작성 일시") LocalDateTime createdAt,
        @Schema(description = "경과 시간", example = "6일 전") String timeAgo,
        @Schema(description = "신고로 가려진 댓글 여부 (본인 조회 시 true 가능, 타인 조회 시 항상 false)") boolean isHidden
) {
    public static CommentSummaryInGroup from(CommunityComments comment) {
        Long parentId = (comment.getParentId() == null || comment.getParentId() == 0L)
                ? null
                : comment.getParentId();
        return new CommentSummaryInGroup(
                comment.getId(),
                parentId,
                comment.getBody(),
                comment.getCreatedAt(),
                TimeAgoUtil.format(comment.getCreatedAt()),
                comment.isHidden()
        );
    }
}
