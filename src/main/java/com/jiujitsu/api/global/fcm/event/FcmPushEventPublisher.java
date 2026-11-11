package com.jiujitsu.api.global.fcm.event;

import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FcmPushEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 푸시 발송 요청. 제목·본문은 발송 지점이 정한다
     * - 커뮤니티: 제목 = 알림 종류 문구(좋아요는 개수 포함), 본문 = 알림함 미리보기와 같은 게시글 제목·댓글 내용 (#196)
     * - 대회 마감: 대회·마감 단계마다 다른 문구
     */
    public void publish(Long recipientUserId, FcmPushType pushType, Map<String, String> data, String title, String body) {
        if (recipientUserId == null || pushType == null) {
            return;
        }
        eventPublisher.publishEvent(new FcmPushRequestedEvent(recipientUserId, pushType, data, title, body));
    }
}
