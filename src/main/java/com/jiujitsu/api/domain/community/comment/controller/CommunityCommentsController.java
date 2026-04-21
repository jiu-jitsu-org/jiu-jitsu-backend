package com.jiujitsu.api.domain.community.comment.controller;

import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsWriteRequest;
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
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.BOARD_NOT_FOUND})
    @GetMapping
    public List<CommunityCommentsResponse> getComments(
            @Parameter(description = "커뮤니티 게시글 ID", example = "1")
            @RequestParam Long contentId
    ) {
        return communityCommentsService.getComments(contentId);
    }

    @Operation(
            summary = "커뮤니티 게시글 댓글 생성",
            description = "현재 커뮤니티 게시물의 댓글을 생성합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.BOARD_NOT_FOUND})
    @PostMapping
    public CommunityCommentsResponse createComment(@RequestBody CommunityCommentsWriteRequest body) {
        return communityCommentsService.createComment(body);
    }
}
