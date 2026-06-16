package com.jiujitsu.api.global.fcm.event;

import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPushEventListener {

    private final UserRepository userRepository;
    private final FcmPushService fcmPushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFcmPushRequested(FcmPushRequestedEvent event) {
        userRepository.findByIdWithAppInfos(event.recipientUserId())
                .ifPresentOrElse(
                        user -> {
                            if (isNoticeEnabled(user, event.pushType())) {
                                fcmPushService.send(user, event.pushType(), event.data());
                            }
                        },
                        () -> log.warn("FCM push skipped: user not found. userId={}", event.recipientUserId())
                );
    }

    private boolean isNoticeEnabled(User user, FcmPushType pushType) {
        UserNoticeSetting setting = user.getUserNoticeSetting();
        if (setting == null) return true;
        return setting.getEnabledByPushType(pushType.getNoticeGroupType());
    }
}
