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

    @Schema(description = "카테고리명", example = "도장 · 오픈매트")
    private String categoryName;

    @Schema(description = "설명 한 줄 (글쓰기 · 피드 카테고리 탭)", example = "도장 고르기, 오픈매트, 승급 · 도장 생활")
    private String description;

    @Schema(description = "이번 주 써 볼 주제. 없으면 null (앱에서 안 보임)", example = "스파링하다 귀가 부었을 때, 어떻게 하셨나요?", nullable = true)
    private String weeklyTopic;

    public BoardCategoryResponse(BoardCategory boardCategory) {
        this.id = boardCategory.getId();
        this.categoryName = boardCategory.getName();
        this.description = boardCategory.getDescription();
        this.weeklyTopic = boardCategory.getWeeklyTopic();
    }
}
