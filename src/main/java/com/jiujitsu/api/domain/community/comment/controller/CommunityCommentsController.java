package com.jiujitsu.api.domain.community.comment.controller;

import com.jiujitsu.api.domain.community.comment.dto.CommentsListRequest;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsWriteRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeResponse;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "community-comments-controller", description = "커뮤니티 댓글 API")
@ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER, ErrorCode.AUTHENTICATION_FAILED})
@RestController
@RequestMapping("/community/comments")
@RequiredArgsConstructor
public class CommunityCommentsController {

    private final CommunityCommentsService communityCommentsService;

    @Operation(
            summary = "커뮤니티 게시글 댓글 목록 조회",
            description = "현재 커뮤니티 게시물의 댓글을 조회합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.CONTENT_NOT_FOUND})
    @GetMapping
    public List<CommunityCommentsResponse> getComments(@ModelAttribute CommentsListRequest request) {
        return communityCommentsService.getComments(request);
    }

    @Operation(
            summary = "커뮤니티 게시글 댓글 생성",
            description = "현재 커뮤니티 게시물의 댓글을 생성합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.CONTENT_NOT_FOUND})
    @PostMapping
    public CommunityCommentsResponse createComment(@RequestBody CommunityCommentsWriteRequest request) {
        return communityCommentsService.createComment(request);
    }

    @Operation(
            summary = "댓글 좋아요/취소",
            description = "댓글 좋아요 등록/취소합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.COMMENT_NOT_FOUND})
    @PostMapping("/like")
    public CommentLikeResponse createCommentLike(@RequestBody CommentLikeRequest request) {
        return communityCommentsService.createCommentLike(request);
    }

    @Operation(
            summary = "댓글 삭제",
            description = "작성한 댓글을 삭제합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.COMMENT_NOT_FOUND})
    @DeleteMapping("/{id}")
    public void deleteComment(
            @Parameter(name = "id", description = "댓글 ID", required = true) @PathVariable(name = "id") Long commentId
    ) {
        communityCommentsService.deleteComment(commentId);
    }
}
