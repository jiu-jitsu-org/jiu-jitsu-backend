package com.jiujitsu.api.domain.community.content.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContentType {
    BOARD,  // 기본 게시판
    BALANCE // 밸런스 게임
}
