package com.jiujitsu.api.domain.community.board.dto;

import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "커뮤니티 카테고리 관리자 응답")
public record AdminBoardCategoryResponse(

        @Schema(description = "카테고리 ID", example = "1")
        Long id,

        @Schema(description = "카테고리 이름", example = "도장 · 오픈매트")
        String name,

        @Schema(description = "설명 한 줄", example = "도장 고르기, 오픈매트, 승급 · 도장 생활")
        String description,

        @Schema(description = "표시 순서", example = "1")
        int displayOrder,

        @Schema(description = "이번 주 써 볼 주제. null이면 앱에서 숨김", nullable = true)
        String weeklyTopic,

        @Schema(description = "사용 여부", example = "true")
        boolean isActive
) {
    public AdminBoardCategoryResponse(BoardCategory category) {
        this(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getWeeklyTopic(),
                category.isActive()
        );
    }
}
