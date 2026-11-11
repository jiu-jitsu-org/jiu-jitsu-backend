package com.jiujitsu.api.domain.community.content.entity;

import com.jiujitsu.api.global.fcm.entity.PushActionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContentType {
    BOARD(PushActionType.BOARD_DETAIL, true),       // 기본 게시판
    BALANCE(PushActionType.BALANCE_DETAIL, false);  // 밸런스 게임

    // 댓글/좋아요 알림의 딥링크 목적지. 컨텐츠 타입이 목적지를 갖고 있어야
    // 같은 푸시 종류라도 대상에 맞는 화면으로 이동한다.
    private final PushActionType pushActionType;

    // 글쓴이에게 "내 게시글" 알림(새 댓글·좋아요)을 보내는가. 밸런스 게임은 운영자가 올린 게임이라
    // 등록한 관리자 계정이 받지 않는다 (#196). 댓글 작성자가 받는 대댓글·댓글 좋아요 알림과는 무관하다.
    private final boolean notifiesWriter;
}
