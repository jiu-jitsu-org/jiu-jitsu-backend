package com.jiujitsu.api.domain.community.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 작성 요청")
public record CommunityCommentsWriteRequest(
        @Schema(description = "게시글 ID", example = "1")
        Long contentId,
        @Schema(description = "부모 댓글 ID (대댓글 아니라면 null 또는 0)", example = "null")
        Long parentId,
        @Schema(description = "댓글 내용", example = "test")
        String body,
        @Schema(description = "대댓글의 답글 버튼으로 쓸 때 그 대댓글 ID (선택). 같은 스레드의 대댓글이어야 하고(없는 댓글·다른 스레드면 400), 답글 알림이 최상위 댓글 작성자 대신 그 대댓글 작성자에게 간다. "
                + "그 대댓글이 지워졌거나 숨겨졌거나 내 대댓글이면 태그 없는 답글로 처리. 태그 없음은 null 또는 0", example = "null", nullable = true)
        Long mentionCommentId
) {

    // AOS 에서 '부모 없음'을 0으로 보내는 경우를 정규화
    public Long normalizedParentId() {
        return (parentId == null || parentId == 0L) ? null : parentId;
    }

    // 태그 없음도 0으로 올 수 있어(parentId 와 같은 관례) null 로 맞춘다
    public Long normalizedMentionCommentId() {
        return (mentionCommentId == null || mentionCommentId == 0L) ? null : mentionCommentId;
    }
}
