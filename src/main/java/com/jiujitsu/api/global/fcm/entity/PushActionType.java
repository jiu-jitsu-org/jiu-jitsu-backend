package com.jiujitsu.api.global.fcm.entity;

import lombok.Getter;

@Getter
public enum PushActionType {
    BOARD_DETAIL;    // 게시물 상세

    public static PushActionType from(String value) {
        for (PushActionType type : PushActionType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PushActionType: " + value);
    }
}
