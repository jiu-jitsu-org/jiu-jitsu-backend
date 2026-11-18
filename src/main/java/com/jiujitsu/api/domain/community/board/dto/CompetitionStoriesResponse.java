package com.jiujitsu.api.domain.community.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "대회 이야기 (대회 상세)")
public record CompetitionStoriesResponse(
        @Schema(description = "이 대회를 고른 글 전체 수 (피드와 같은 제외). 대회 상세 '이야기 N' · 'N개 모두 보기'", example = "12")
        long totalCount,
        @Schema(description = "최신 글 최대 5개. 항목은 게시글 목록과 같은 형식, 없으면 빈 배열")
        List<BoardListResponse> stories
) { }
