package com.jiujitsu.api.domain.community.board.controller;

import com.jiujitsu.api.domain.community.board.dto.CompetitionStoriesResponse;
import com.jiujitsu.api.domain.community.board.service.BoardService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 대회 상세의 "대회 이야기". 경로는 대회 아래지만 게시글을 다루므로 커뮤니티(board)에 둔다 — 대회 도메인은 커뮤니티를 모른다
 */
@Tag(name = "competition-controller", description = "대회 API")
@CommonApiResponses
@RestController
@RequestMapping("/competitions")
@RequiredArgsConstructor
public class CompetitionStoryController {

    private final BoardService boardService;

    @Operation(
            summary = "대회 이야기 (대회 상세)",
            description = "이 대회를 고른 게시글 중 최신 5개와 전체 글 수를 반환합니다. 비로그인도 조회할 수 있습니다.\n\n"
                    + "- stories: 최신순 최대 5개. 항목은 게시글 목록과 같은 형식, 글이 없으면 빈 배열\n"
                    + "- totalCount: 이 대회를 고른 글 전체 수 ('이야기 N' · 'N개 모두 보기'). 카테고리와 상관없이 셈 — 같은 목록은 GET /board?boardListType=FEED&competitionId=\n"
                    + "- 피드와 같은 글이 빠집니다 (숨김, 차단한 작성자, 내가 신고·숨긴 글)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND})
    @GetMapping("/{competitionId}/stories")
    public CompetitionStoriesResponse getStories(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId
    ) {
        return boardService.getCompetitionStories(competitionId);
    }
}
