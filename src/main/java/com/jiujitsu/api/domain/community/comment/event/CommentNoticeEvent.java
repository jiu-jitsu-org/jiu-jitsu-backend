package com.jiujitsu.api.domain.community.comment.event;

import com.jiujitsu.api.global.fcm.entity.FcmPushType;

import java.util.Map;

public record CommentNoticeEvent(
        Long recipientUserId,
        Long contentId,
        FcmPushType pushType,
        Map<String, String> pushData
) {}
