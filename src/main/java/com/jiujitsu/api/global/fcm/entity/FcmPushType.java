package com.jiujitsu.api.global.fcm.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.jiujitsu.api.global.fcm.entity.NoticeGroupType.COMMUNITY;
import static com.jiujitsu.api.global.fcm.entity.PushActionType.BOARD_DETAIL;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum FcmPushType {

    NEW_COMMENTS("내 게시글에 새로운 댓글이 달렸어요.", null, BOARD_DETAIL, COMMUNITY),
    NEW_CHILD_COMMENTS("내 댓글에 답글이 달렸어요.", null, BOARD_DETAIL, COMMUNITY),
    CONTENTS_LIKE("내 게시글에 좋아요가 달렸어요.", null, BOARD_DETAIL, COMMUNITY),
    COMMENTS_LIKE("내 댓글에 좋아요가 달렸어요.", null, BOARD_DETAIL, COMMUNITY);

    private String title;
    private String body;
    private PushActionType actionType;
    private NoticeGroupType noticeGroupType;
}
