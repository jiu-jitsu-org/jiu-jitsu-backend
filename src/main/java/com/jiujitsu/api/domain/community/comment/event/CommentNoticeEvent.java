package com.jiujitsu.api.domain.community.comment.event;

import com.jiujitsu.api.global.fcm.entity.FcmPushType;

import java.util.Map;

public record CommentNoticeEvent(
        Long recipientUserId,
        Long contentId,
        FcmPushType pushType,
        Map<String, String> pushData,
        Long senderUserId,
        Long refId,         // 댓글/대댓글 ID
        String pushBody     // 푸시 본문: 새 댓글 = 게시글 제목, 대댓글 = 대댓글이 달린 내 댓글 (알림함 미리보기와 같음, #196)
) {}
