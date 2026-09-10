package com.jiujitsu.api.global.fcm.entity;

import lombok.Getter;

/**
 * 푸시/알림함 탭 시 이동할 화면.
 * 값은 푸시 종류가 아니라 대상 컨텐츠 타입이 결정한다 ({@link com.jiujitsu.api.domain.community.content.entity.ContentType}).
 */
@Getter
public enum PushActionType {
    BOARD_DETAIL,    // 게시물 상세
    BALANCE_DETAIL;  // 밸런스 게임 상세

    public static PushActionType from(String value) {
        for (PushActionType type : PushActionType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PushActionType: " + value);
    }
}
