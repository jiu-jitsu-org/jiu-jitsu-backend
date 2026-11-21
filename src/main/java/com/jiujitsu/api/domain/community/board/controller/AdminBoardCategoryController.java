package com.jiujitsu.api.domain.community.board.controller;

import com.jiujitsu.api.domain.community.board.dto.AdminBoardCategoryRequest;
import com.jiujitsu.api.domain.community.board.dto.AdminBoardCategoryResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardCategoryResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardCategoryWeeklyTopicRequest;
import com.jiujitsu.api.domain.community.board.service.BoardCategoryService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "admin-board-category-controller", description = "관리자 커뮤니티 카테고리 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/board/categories")
@RequiredArgsConstructor
public class AdminBoardCategoryController {

    private final BoardCategoryService boardCategoryService;

    @Operation(summary = "카테고리 전체 목록 조회", description = "사용 안 함 카테고리를 포함해 전체 목록을 반환합니다.")
    @GetMapping
    public List<AdminBoardCategoryResponse> getAllCategories() {
        return boardCategoryService.getAllCategoriesForAdmin();
    }

    @Operation(summary = "카테고리 생성", description = "커뮤니티 카테고리를 새로 만듭니다. 이름은 중복 불가.")
    @ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER})
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminBoardCategoryResponse createCategory(@Valid @RequestBody AdminBoardCategoryRequest request) {
        return boardCategoryService.createCategoryByAdmin(request);
    }

    @Operation(summary = "카테고리 수정", description = "이름, 설명, 순서, 써 볼 주제, 사용 여부를 한 번에 수정합니다.")
    @ApiErrorCodeExamples({ErrorCode.BOARD_CATEGORY_NOT_FOUND, ErrorCode.WRONG_PARAMETER})
    @PutMapping("/{categoryId}")
    public AdminBoardCategoryResponse updateCategory(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable Long categoryId,
            @Valid @RequestBody AdminBoardCategoryRequest request) {
        return boardCategoryService.updateCategoryByAdmin(categoryId, request);
    }

    @Operation(summary = "이번 주 써 볼 주제 바꾸기",
            description = "카테고리의 '이번 주 써 볼 주제'를 바꿉니다. 카테고리 목록(GET /board/category)의 weeklyTopic 으로 나갑니다. "
                    + "null · 빈 값 · 공백만이면 주제를 지웁니다(앱에서 안 보임). 100자까지.")
    @ApiErrorCodeExamples({ErrorCode.BOARD_CATEGORY_NOT_FOUND, ErrorCode.WRONG_PARAMETER})
    @PutMapping("/{categoryId}/weekly-topic")
    public BoardCategoryResponse changeWeeklyTopic(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable Long categoryId,
            @Valid @RequestBody(required = false) BoardCategoryWeeklyTopicRequest request) {
        return boardCategoryService.changeWeeklyTopic(categoryId, request);
    }
}
