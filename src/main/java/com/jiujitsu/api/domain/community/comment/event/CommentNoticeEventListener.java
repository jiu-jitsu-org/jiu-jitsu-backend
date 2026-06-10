package com.jiujitsu.api.domain.community.comment.event;

import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
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
public class CommentNoticeEventListener {

    private final NoticeService noticeService;
    private final FcmPushEventPublisher fcmPushEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CommentNoticeEvent event) {
        if (!noticeService.isContentNoticeEnabled(event.recipientUserId(), event.contentId())) {
            return;
        }
        fcmPushEventPublisher.publish(event.recipientUserId(), event.pushType(), event.pushData());
        noticeService.saveNotice(event.recipientUserId(), event.pushType(), event.pushData());
    }
}
