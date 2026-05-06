package com.jiujitsu.api.global.fcm.event;

import com.jiujitsu.api.domain.user.repository.UserRepository;
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
                        user -> fcmPushService.send(user, event.pushType()),
                        () -> log.warn("FCM push skipped: user not found. userId={}", event.recipientUserId())
                );
    }
}
