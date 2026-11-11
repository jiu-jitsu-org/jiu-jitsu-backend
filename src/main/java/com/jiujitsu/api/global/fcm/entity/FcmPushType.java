package com.jiujitsu.api.global.fcm.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.jiujitsu.api.global.fcm.entity.NoticeGroupType.COMMUNITY;
import static com.jiujitsu.api.global.fcm.entity.NoticeGroupType.COMPETITION;

/**
 * 푸시 종류. 이동할 화면({@link PushActionType})은 대상 컨텐츠 타입이 결정하므로 여기서 갖지 않는다.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum FcmPushType {

    NEW_COMMENTS("내 게시글에 새로운 댓글이 달렸어요.", null, COMMUNITY),
    NEW_CHILD_COMMENTS("내 댓글에 답글이 달렸어요.", null, COMMUNITY),
    CONTENTS_LIKE("내 게시글에 좋아요가 달렸어요.", null, COMMUNITY),
    COMMENTS_LIKE("내 댓글에 좋아요가 달렸어요.", null, COMMUNITY),
    // 대회 마감 전날 알림. 제목·본문은 대회·할인 단계마다 달라 발송 지점이 정한다 (여기 값은 기본 문구)
    COMPETITION_EARLY_BIRD_DEADLINE("얼리버드 마감 내일", null, COMPETITION),
    COMPETITION_REGISTRATION_DEADLINE("접수 마감 내일", null, COMPETITION);

    private String title;
    private String body;
    private NoticeGroupType noticeGroupType;
}
