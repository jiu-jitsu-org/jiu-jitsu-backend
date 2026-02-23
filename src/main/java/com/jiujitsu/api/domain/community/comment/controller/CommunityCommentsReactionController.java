package com.jiujitsu.api.domain.community.comment.controller;

import com.jiujitsu.api.domain.community.comment.dto.reaction.CommunityCommentReactionRequest;
import com.jiujitsu.api.domain.community.comment.dto.reaction.CommunityCommentReactionResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommunityCommentReactionType;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsReactionService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "community-comments-reaction-controller", description = "커뮤니티 댓글 반응 API (Like, DisLike")
@ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER, ErrorCode.AUTHENTICATION_FAILED})
@RestController
@RequestMapping("/community/comments/{commentId}")
@RequiredArgsConstructor
public class CommunityCommentsReactionController {

    private final CommunityCommentsReactionService reactionService;

    @PostMapping("/reaction")
    public CommunityCommentReactionResponse reaction(
            @PathVariable Long commentId,
            @RequestBody CommunityCommentReactionRequest requestBody,
            @AuthenticationPrincipal Long userId
    ) {
        var type = CommunityCommentReactionType.valueOf(requestBody.type());
        var summary = reactionService.toggle(commentId, userId, type);
        return new CommunityCommentReactionResponse(
                summary.likeCount(),
                summary.dislikeCount(),
                summary.myType() == null ? null : summary.myType().name()
        );
    }
}
