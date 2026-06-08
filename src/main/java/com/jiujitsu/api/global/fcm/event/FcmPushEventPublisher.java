package com.jiujitsu.api.global.fcm.event;

import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FcmPushEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final AuthenticationFacade authenticationFacade;

    public void publish(Long recipientUserId, FcmPushType pushType, Map<String, String> data) {
        if (recipientUserId == null || pushType == null) {
            return;
        }

        // 수신 거부 여부 체크
        UserNoticeSetting userNoticeSetting = authenticationFacade.getCurrentUser().getUserNoticeSetting();
        if (!userNoticeSetting.getEnabledByPushType(pushType.getNoticeGroupType())) {
            return;
        }

        eventPublisher.publishEvent(new FcmPushRequestedEvent(recipientUserId, pushType, data));
    }
}
