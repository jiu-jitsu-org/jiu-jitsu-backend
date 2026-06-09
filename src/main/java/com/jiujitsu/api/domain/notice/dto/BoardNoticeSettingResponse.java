package com.jiujitsu.api.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시물 알림 설정 응답")
public record BoardNoticeSettingResponse(
        @Schema(description = "게시글 ID", example = "1") Long boardId,
        @Schema(description = "알림 활성화 여부 (댓글/좋아요/게시물 전체)", example = "true") Boolean enabled
) {
}
