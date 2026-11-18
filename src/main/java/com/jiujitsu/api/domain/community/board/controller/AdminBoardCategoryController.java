package com.jiujitsu.api.domain.community.board.controller;

import com.jiujitsu.api.domain.community.board.dto.BoardCategoryResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardCategoryWeeklyTopicRequest;
import com.jiujitsu.api.domain.community.board.service.BoardCategoryService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin-board-category-controller", description = "관리자 커뮤니티 카테고리 API")
@CommonApiResponses
@RestController
@RequestMapping("/admin/board/categories")
@RequiredArgsConstructor
public class AdminBoardCategoryController {

    private final BoardCategoryService boardCategoryService;

    @Operation(summary = "이번 주 써 볼 주제 바꾸기",
            description = "카테고리의 '이번 주 써 볼 주제'를 바꿉니다. 카테고리 목록(GET /board/category)의 weeklyTopic 으로 나갑니다. "
                    + "null · 빈 값 · 공백만이면 주제를 지웁니다(앱에서 안 보임). 100자까지. (ROLE_ADMIN 필요)")
    @ApiErrorCodeExamples({ErrorCode.BOARD_CATEGORY_NOT_FOUND, ErrorCode.WRONG_PARAMETER})
    @PutMapping("/{categoryId}/weekly-topic")
    public BoardCategoryResponse changeWeeklyTopic(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable Long categoryId,
            @Valid @RequestBody(required = false) BoardCategoryWeeklyTopicRequest request) {
        return boardCategoryService.changeWeeklyTopic(categoryId, request);
    }
}
