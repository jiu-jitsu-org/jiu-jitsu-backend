package com.jiujitsu.api.domain.notice.factory;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NoticeFactory {
    /**
     * Create Notice
     * 딥링크 목적지는 발송 지점이 대상 컨텐츠 기준으로 담아준 data("type") 를 그대로 쓴다.
     * FcmPushType 에서 읽으면 푸시 종류로 목적지가 고정되어 밸런스 게임 알림이 게시글 상세로 향한다.
     */
    public Notice createNotice(Long userId, FcmPushType pushType, Map<String, String> data) {
         return Notice.builder()
                .userId(userId)
                .pushType(pushType)
                .title(pushType.getTitle())
                .body(pushType.getBody())
                .pushActionType(PushActionType.from(data.get("type")))
                .data(data.get("data"))
                .isRead(false)
                .build();
    }
}
