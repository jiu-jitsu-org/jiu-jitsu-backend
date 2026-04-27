package com.jiujitsu.api.domain.community.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "커뮤니티 목록 응답")
public record BoardListResponse(
        @Schema(description = "게시글 ID", example = "1") Long id,
        @Schema(description = "카테고리 ID", example = "1") Long categoryId,
        @Schema(description = "카테고리명", example = "자유게시판") String categoryName,
        @Schema(description = "제목", example = "타이틀입니다.") String title,
        @Schema(description = "내용", example = "내용입니다.") String body,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt,
        @Schema(description = "상위 댓글 수 (대댓글 제외)", example = "5") Long commentCount,
        @Schema(description = "좋아요 수", example = "17") Long likeCount,
        @Schema(description = "이미지 URL 리스트") List<String> imageUrlList
) {
}
