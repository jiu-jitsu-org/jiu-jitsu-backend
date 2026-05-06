package com.jiujitsu.api.global.fcm.event;

import com.jiujitsu.api.global.fcm.entity.FcmPushType;

/**
 * 푸시 발송 요청. 도메인 서비스는 이 이벤트만 발행하고, 실제 FCM 호출은 리스너에서 처리합니다.
 */
public record FcmPushRequestedEvent(Long recipientUserId, FcmPushType pushType) {
}
