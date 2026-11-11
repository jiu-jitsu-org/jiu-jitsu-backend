package com.jiujitsu.api.domain.notice.event;

import com.jiujitsu.api.global.fcm.entity.FcmPushType;

import java.util.Map;

/**
 * 좋아요가 눌림 (#196). 좋아요가 커밋된 뒤 LikeNoticeEventListener 가 카드·푸시를 처리한다
 *
 * @param recipientUserId 좋아요 받은 글·댓글 작성자
 * @param contentId       게시글·밸런스 게임 contentId (알림 설정·숨김·신고 확인, 딥링크)
 * @param commentId       댓글 좋아요면 좋아요 받은 댓글, 게시글 좋아요면 null
 * @param pushType        CONTENTS_LIKE / COMMENTS_LIKE
 * @param pushData        딥링크 (type = 대상 컨텐츠 화면, data = contentId)
 */
public record LikeNoticeEvent(
        Long recipientUserId,
        Long contentId,
        Long commentId,
        FcmPushType pushType,
        Map<String, String> pushData
) {
    // 카드 키: 게시글 좋아요는 contentId, 댓글 좋아요는 commentId 기준으로 한 장
    public String groupKey() {
        return pushType.name() + ":" + (commentId != null ? commentId : contentId);
    }
}
