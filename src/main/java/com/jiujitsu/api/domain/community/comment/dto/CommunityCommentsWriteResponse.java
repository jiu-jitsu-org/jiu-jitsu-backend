package com.jiujitsu.api.domain.community.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 작성 수정 응답")
public record CommunityCommentsWriteResponse(
        @Schema(description = "성공여부", example = "true false")
        boolean isSuccess
) { }
