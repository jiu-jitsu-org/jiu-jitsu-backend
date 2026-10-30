package com.jiujitsu.api.domain.community.content.entity;

import com.jiujitsu.api.global.fcm.entity.PushActionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContentType {
    BOARD(PushActionType.BOARD_DETAIL),      // 기본 게시판
    BALANCE(PushActionType.BALANCE_DETAIL);  // 밸런스 게임

    // 댓글/좋아요 알림의 딥링크 목적지. 컨텐츠 타입이 목적지를 갖고 있어야
    // 같은 푸시 종류라도 대상에 맞는 화면으로 이동한다.
    private final PushActionType pushActionType;
}
