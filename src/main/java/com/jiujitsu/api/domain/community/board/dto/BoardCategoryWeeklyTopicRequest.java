package com.jiujitsu.api.domain.community.board.dto;

import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "카테고리 이번 주 써 볼 주제 바꾸기 (관리자)")
public record BoardCategoryWeeklyTopicRequest(
        @Size(max = BoardCategory.WEEKLY_TOPIC_MAX_LENGTH, message = "주제는 100자 이하여야 합니다.")
        @Schema(description = "주제 (100자). null · 빈 값 · 공백만이면 주제를 지움", example = "스파링하다 귀가 부었을 때, 어떻게 하셨나요?", nullable = true)
        String weeklyTopic
) { }
