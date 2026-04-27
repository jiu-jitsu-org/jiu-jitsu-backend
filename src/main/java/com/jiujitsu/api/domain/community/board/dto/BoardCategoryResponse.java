package com.jiujitsu.api.domain.community.board.dto;

import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커뮤니티 카테고리 응답")
public class BoardCategoryResponse {
    @Schema(description = "카테고리 ID", example = "1")
    private Long id;

    @Schema(description = "카테고리명", example = "자유게시판")
    private String categoryName;

    public BoardCategoryResponse(BoardCategory boardCategory) {
        this.id = boardCategory.getId();
        this.categoryName = boardCategory.getName();
    }
}
