package com.jiujitsu.api.domain.community.comment.controller;

import com.jiujitsu.api.domain.community.comment.dto.CommentsListRequest;
import com.jiujitsu.api.domain.community.comment.dto.CommentsSortType;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsWriteRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeResponse;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "community-comments-controller", description = "커뮤니티 댓글 API")
@CommonApiResponses
@RestController
@RequestMapping("/community/comments")
@RequiredArgsConstructor
public class CommunityCommentsController {

    private final CommunityCommentsService communityCommentsService;

    @Operation(
            summary = "커뮤니티 게시글 댓글 목록 조회",
            description = "현재 커뮤니티 게시물의 댓글을 조회합니다. 대댓글은 sortType 기준 3개만 포함하며, 나머지는 대댓글 추가 조회로 가져옵니다. 차단한 작성자의 댓글/대댓글은 제외하지 않고 isBlocked=true로 내려가며, 본문과 작성자 정보는 마스킹됩니다."
    )
    @ApiErrorCodeExamples({ErrorCode.CONTENT_NOT_FOUND})
    @GetMapping
    public List<CommunityCommentsResponse> getComments(@ModelAttribute CommentsListRequest request) {
        return communityCommentsService.getComments(request);
    }

    @Operation(
            summary = "대댓글 추가 조회",
            description = "댓글 ID 기준으로 목록 미리보기 3개 이후의 대댓글을 10개씩 페이징 조회합니다. sortType은 댓글 목록과 동일하게 적용하고, page는 0부터입니다."
    )
    @ApiErrorCodeExamples({ErrorCode.COMMENT_NOT_FOUND, ErrorCode.WRONG_PARAMETER})
    @GetMapping("/{id}/replies")
    public Slice<CommunityCommentsResponse> getReplies(
            @Parameter(name = "id", description = "부모 댓글 ID", required = true) @PathVariable(name = "id") Long commentId,
            @Parameter(name = "page", description = "페이지 번호 (0부터)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(name = "sortType", description = "댓글 정렬 조건", example = "CREATE_DESC")
            @RequestParam(name = "sortType", required = false) CommentsSortType sortType
    ) {
        return communityCommentsService.getReplies(commentId, page, sortType);
    }

    @Operation(
            summary = "커뮤니티 게시글 댓글 생성",
            description = "현재 커뮤니티 게시물의 댓글을 생성합니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.CONTENT_NOT_FOUND})
    @PostMapping
    public CommunityCommentsResponse createComment(@RequestBody CommunityCommentsWriteRequest request) {
        return communityCommentsService.createComment(request);
    }

    @Operation(
            summary = "댓글 좋아요/취소",
            description = "댓글 좋아요 등록/취소합니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.CONTENT_NOT_FOUND})
    @PostMapping("/like")
    public CommentLikeResponse createCommentLike(@RequestBody CommentLikeRequest request) {
        return communityCommentsService.createCommentLike(request);
    }

    @Operation(
            summary = "댓글 삭제",
            description = "작성한 댓글을 삭제합니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.CONTENT_NOT_FOUND})
    @DeleteMapping("/{id}")
    public void deleteComment(
            @Parameter(name = "id", description = "댓글 ID", required = true) @PathVariable(name = "id") Long commentId
    ) {
        communityCommentsService.deleteComment(commentId);
    }
}
