package com.jiujitsu.api.domain.community.dto.comments;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "등록된 댓글 수정 요청")
public record CommunityCommentsUpdateRequest(
        @Schema(description = "수정할 댓글 ID", example = "")
        Long id,
        @Schema(description = "댓글 내용", example = "")
        String body
) {
}
