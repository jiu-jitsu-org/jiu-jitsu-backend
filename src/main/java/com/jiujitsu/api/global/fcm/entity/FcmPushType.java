package com.jiujitsu.api.global.fcm.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum FcmPushType {

    NEW_COMMENTS("내 게시글에 새로운 댓글이 달렸어요.", "내 게시글에 새로운 댓글이 달렸어요.");

    private String title;
    private String body;
}
