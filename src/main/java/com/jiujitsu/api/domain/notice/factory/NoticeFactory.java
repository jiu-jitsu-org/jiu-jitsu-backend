package com.jiujitsu.api.domain.notice.factory;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NoticeFactory {
    /**
     * Create Notice
     */
    public Notice createNotice(Long userId, FcmPushType pushType, Map<String, String> data) {
         return Notice.builder()
                .userId(userId)
                .pushType(pushType)
                .title(pushType.getTitle())
                .body(pushType.getBody())
                .pushActionType(pushType.getActionType())
                .data(data.get("data"))
                .isRead(false)
                .build();
    }
}
