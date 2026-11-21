package com.jiujitsu.api.domain.community.board.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "커뮤니티 카테고리 생성·수정 요청")
public record AdminBoardCategoryRequest(

        @Schema(description = "카테고리 이름", example = "도장 · 오픈매트")
        @NotBlank
        @Size(max = 64)
        String name,

        @Schema(description = "설명 한 줄. 앱 글쓰기 화면에 보임", example = "도장 고르기, 오픈매트, 승급 · 도장 생활")
        @Size(max = 100)
        String description,

        @Schema(description = "표시 순서. 작을수록 앞", example = "1")
        @NotNull
        Integer displayOrder,

        @Schema(description = "이번 주 써 볼 주제. null·빈 값이면 앱에서 숨김", example = "스파링하다 귀가 부었을 때, 어떻게 하셨나요?", nullable = true)
        @Size(max = 100)
        String weeklyTopic,

        @Schema(description = "사용 여부", example = "true")
        @JsonProperty("isActive")
        @NotNull
        Boolean isActive
) {}
