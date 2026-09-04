package com.jiujitsu.api.domain.community.board.controller;

import com.jiujitsu.api.domain.community.board.dto.*;
import com.jiujitsu.api.domain.community.board.service.BoardCategoryService;
import com.jiujitsu.api.domain.community.board.service.BoardHideService;
import com.jiujitsu.api.domain.community.board.service.BoardService;
import com.jiujitsu.api.domain.community.content.dto.ContentLikeResponse;
import com.jiujitsu.api.domain.community.content.dto.ContentSaveResponse;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "board-controller", description = "커뮤니티 게시글 API")
@CommonApiResponses
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final BoardCategoryService boardCategoryService;
    private final BoardHideService boardHideService;
    private final ContentService contentService;

    /**
     * 카테고리
     */
    @Operation(summary = "카테고리 목록 조회", description = "카테고리 목록 조회(임시)")
    @GetMapping("/category")
    public List<BoardCategoryResponse> getCategory() {
        return boardCategoryService.getCategory();
    }


    /**
     * 게시물
     */
    @Operation(summary = "게시글 목록 조회", description = "카테고리별 페이징 게시글 목록을 조회합니다.")
    @GetMapping
    public Page<BoardListResponse> getList(
            @ModelAttribute BoardListRequest boardListRequest,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return boardService.getList(boardListRequest, pageable);
    }

    @Operation(summary = "게시글 단건 조회", description = "ID로 게시글을 조회합니다.")
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND})
    @GetMapping("/{id}")
    public BoardResponse getById(
            @Parameter(name = "id", description = "게시글 ID", required = true) @PathVariable(name = "id") Long id
    ) {
        return boardService.getById(id);
    }

    @Operation(summary = "게시글 생성", description = "새 게시글을 생성합니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BOARD_CATEGORY_NOT_FOUND, ErrorCode.REQUIRED_PARAMETER})
    @PostMapping
    public BoardResponse create(@Valid @RequestBody BoardCreateRequest request) {
        return boardService.create(request);
    }

    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND, ErrorCode.BOARD_CATEGORY_NOT_FOUND})
    @PutMapping("/{id}")
    public BoardResponse update(
            @Parameter(name = "id", description = "게시글 ID", required = true) @PathVariable Long id,
            @RequestBody BoardUpdateRequest request
    ) {
        return boardService.update(id, request);
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND})
    @DeleteMapping("/{id}")
    public void delete(
            @Parameter(name = "id", description = "게시글 ID", required = true) @PathVariable(name = "id") Long id
    ) {
        boardService.delete(id);
    }

    @Operation(summary = "게시물 좋아요 등록/취소", description = "게시물 좋아요를 등록/취소합니다. 컨텐츠 단위 기능이라 밸런스 게임의 contentId 로도 호출할 수 있습니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.CONTENT_NOT_FOUND})
    @PutMapping("/like/{id}")
    public ContentLikeResponse like(
            @Parameter(name = "id", description = "컨텐츠 ID (게시글/밸런스 게임 공통)", required = true) @PathVariable(name = "id") Long id
    ) {
        return contentService.like(id);
    }

    @Operation(summary = "게시물 저장/취소", description = "게시물을 저장/취소합니다. 게시글만 지원하며, 밸런스 게임 등 다른 컨텐츠는 저장할 수 없습니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.CONTENT_NOT_FOUND, ErrorCode.CONTENT_SAVE_NOT_SUPPORTED})
    @PutMapping("/save/{id}")
    public ContentSaveResponse save(
            @Parameter(name = "id", description = "컨텐츠 ID (게시글/밸런스 게임 공통)", required = true) @PathVariable(name = "id") Long id
    ) {
        return contentService.save(id);
    }

    @Operation(summary = "게시물 숨김/숨김해제", description = "게시물을 숨기거나 숨김 해제합니다. (true = 숨김, false = 숨김해제)")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND, ErrorCode.SELF_HIDE_NOT_ALLOWED})
    @PutMapping("/hide/{id}")
    public Boolean hide(
            @Parameter(name = "id", description = "게시글 ID", required = true) @PathVariable(name = "id") Long id
    ) {
        return boardHideService.toggleHide(id);
    }

    @Operation(summary = "내가 작성한 글 조회", description = "내가 작성한 글 목록을 조회합니다.")
    @LoginErrorExamples
    @GetMapping("/write")
    public Page<BoardListResponse> writeList(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return boardService.getWriteList(pageable);
    }

    @Operation(summary = "내가 저장한 글 조회", description = "내가 저장한 글 목록을 조회합니다.")
    @LoginErrorExamples
    @GetMapping("/save")
    public Page<BoardListResponse> saveList(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return boardService.getSaveList(pageable);
    }
}
