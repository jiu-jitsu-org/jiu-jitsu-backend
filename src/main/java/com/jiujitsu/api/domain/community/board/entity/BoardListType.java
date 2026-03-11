package com.jiujitsu.api.domain.community.board.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BoardListType {
    FEED,  // 피드
    CATEGORY, // 카테고리
    SEARCH // 검색
}
