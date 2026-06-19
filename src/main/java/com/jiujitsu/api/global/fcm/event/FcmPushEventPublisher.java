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

    public void publish(Long recipientUserId, FcmPushType pushType, Map<String, String> data) {
        if (recipientUserId == null || pushType == null) {
            return;
        }
        eventPublisher.publishEvent(new FcmPushRequestedEvent(recipientUserId, pushType, data));
    }
}
