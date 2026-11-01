package com.jiujitsu.api.domain.community.comment.dto;

import com.jiujitsu.api.domain.community.board.entity.Board;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 묶음의 원글 요약")
public record BoardSummaryInGroup(
        @Schema(description = "게시글 ID (content_id)") Long id,
        @Schema(description = "게시글 제목") String title
) {
    public static BoardSummaryInGroup from(Board board) {
        return new BoardSummaryInGroup(
                board.getContent().getId(),
                board.getTitle()
        );
    }
}
