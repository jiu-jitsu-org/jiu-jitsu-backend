package com.jiujitsu.api.domain.community.board.dto;

import com.jiujitsu.api.domain.community.board.entity.BoardListType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "게시글 목록 검색 조건")
public record BoardListRequest(
        @NotNull(message = "목록 타입은 필수입니다.")
        @Schema(description = "목록 타입 코드", example = "FEED", requiredMode = Schema.RequiredMode.REQUIRED)
        BoardListType boardListType,
        @Schema(description = "카테고리 ID", example = "1")
        Long categoryId,
        @Schema(description = "검색어 (SEARCH). 띄어쓰기로 나눈 모든 단어가 든 글, 비었거나 공백만이면 빈 페이지", example = "오픈매트 강남")
        String searchKeyword,
        @Schema(description = "대회 ID (선택). 주면 이 대회를 고른 글만 — 목록 종류와 같이 씀. 대회 이야기와 같은 범위는 FEED + competitionId. 공개 대회만, 아니면 404 COMPETITION_NOT_FOUND")
        Long competitionId
) {

}
