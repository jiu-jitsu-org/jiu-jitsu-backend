package com.jiujitsu.api.domain.community.board.controller;

import com.jiujitsu.api.domain.community.board.dto.*;
import com.jiujitsu.api.domain.community.board.service.BoardCategoryService;
import com.jiujitsu.api.domain.community.board.service.BoardHideService;
import com.jiujitsu.api.domain.community.board.service.BoardService;
import com.jiujitsu.api.domain.community.content.dto.ContentLikeResponse;
import com.jiujitsu.api.domain.community.content.dto.ContentViewResponse;
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

    @Operation(summary = "게시글 단건 조회", description = "ID로 게시글을 조회합니다.\n\n"
            + "- 이 조회로는 조회수가 오르지 않습니다. 웹은 상세 화면을 열 때 POST /board/{id}/views 를 한 번 부릅니다 (#249)")
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND})
    @GetMapping("/{id}")
    public BoardResponse getById(
            @Parameter(name = "id", description = "컨텐츠 ID (게시글의 contentId)", required = true) @PathVariable(name = "id") Long id
    ) {
        return boardService.getById(id);
    }

    @Operation(summary = "게시글 조회수 올리기", description = "상세 화면을 열 때 한 번 부릅니다. 조회수를 1 올리고 올린 뒤 값을 반환합니다.\n\n"
            + "- 비로그인도 부를 수 있고, 부를 때마다 올라갑니다 (중복 방지 없음)\n"
            + "- 작성자 본인은 첫 1회만 셉니다 (그 뒤로는 올리지 않고 지금 값을 반환)\n"
            + "- 상세 조회와 같은 기준으로 볼 수 없는 글(없음 · 숨김 · 내가 신고)은 BOARD_NOT_FOUND")
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND})
    @PostMapping("/{id}/views")
    public ContentViewResponse increaseView(
            @Parameter(name = "id", description = "컨텐츠 ID (게시글의 contentId)", required = true) @PathVariable Long id
    ) {
        return boardService.increaseView(id);
    }

    @Operation(summary = "게시글 생성", description = "새 게시글을 생성합니다. competitionId 로 공개 대회를 연결할 수 있습니다(선택, 카테고리와 무관).")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BOARD_CATEGORY_NOT_FOUND, ErrorCode.REQUIRED_PARAMETER, ErrorCode.COMPETITION_NOT_FOUND})
    @PostMapping
    public BoardResponse create(@Valid @RequestBody BoardCreateRequest request) {
        return boardService.create(request);
    }

    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다.\n\n"
            + "- categoryId·title·body·tags 는 null 이면 그대로 둡니다\n"
            + "- competitionId 는 다릅니다: 보낸 값으로 교체하고 null·생략이면 연결을 해제합니다. 수정 화면은 상세 응답의 competitionId 를 항상 다시 보냅니다\n"
            + "- 지금 연결된 대회 id 를 그대로 보내면 대회가 비공개로 바뀌었어도 연결이 유지됩니다")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND, ErrorCode.BOARD_CATEGORY_NOT_FOUND, ErrorCode.COMPETITION_NOT_FOUND})
    @PutMapping("/{id}")
    public BoardResponse update(
            @Parameter(name = "id", description = "컨텐츠 ID (게시글의 contentId)", required = true) @PathVariable Long id,
            @Valid @RequestBody BoardUpdateRequest request
    ) {
        return boardService.update(id, request);
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND})
    @DeleteMapping("/{id}")
    public void delete(
            @Parameter(name = "id", description = "컨텐츠 ID (게시글의 contentId)", required = true) @PathVariable(name = "id") Long id
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
            @Parameter(name = "id", description = "컨텐츠 ID (게시글의 contentId)", required = true) @PathVariable(name = "id") Long id
    ) {
        return boardHideService.toggleHide(id);
    }

    @Operation(summary = "특정 사용자가 쓴 글 목록", description = "특정 사용자가 작성한 글 목록을 조회합니다. 비로그인 가능. 차단 관계이면 404 반환.")
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND})
    @GetMapping("/user/{userId}")
    public Page<BoardListResponse> getUserBoardList(
            @Parameter(name = "userId", description = "조회할 사용자 ID", required = true) @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return boardService.getUserBoardList(userId, pageable);
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
