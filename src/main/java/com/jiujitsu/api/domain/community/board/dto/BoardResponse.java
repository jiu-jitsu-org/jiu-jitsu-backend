package com.jiujitsu.api.domain.community.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커뮤니티 게시글 응답")
public class BoardResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "Content ID (1:1)", example = "1")
    private Long contentId;

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "카테고리명", example = "자유게시판")
    private String categoryName;

    @Schema(description = "제목", example = "타이틀입니다.")
    private String title;

    @Schema(description = "내용", example = "내용입니다.")
    private String body;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;

    @Schema(description = "상위 댓글 수 (대댓글 제외)", example = "5")
    private Long commentCount;
}
