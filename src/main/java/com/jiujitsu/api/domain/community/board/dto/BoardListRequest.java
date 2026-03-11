package com.jiujitsu.api.domain.community.board.dto;

import com.jiujitsu.api.domain.community.board.entity.BoardListType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "게시글 목록 검색 조건")
public class BoardListRequest {
    @NotNull(message = "목록 타입 코드")
    @Schema(description = "목록 타입 코드", example = "FEED", requiredMode = Schema.RequiredMode.REQUIRED)
    private BoardListType boardListType;

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "검색 키워드", example = "검색어")
    private String searchKeyword;
}
