package com.jiujitsu.api.global.fcm.event;

import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FcmPushEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(Long recipientUserId, FcmPushType pushType) {
        if (recipientUserId == null || pushType == null) {
            return;
        }
        eventPublisher.publishEvent(new FcmPushRequestedEvent(recipientUserId, pushType));
    }
}
