package com.jiujitsu.api.domain.community.comment.event;

import com.jiujitsu.api.domain.community.board.repository.BoardHideRepository;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.repository.ReportRepository;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.repository.UserBlockRepository;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommentNoticeEventListener {

    private final NoticeService noticeService;
    private final FcmPushEventPublisher fcmPushEventPublisher;
    private final UserBlockRepository userBlockRepository;
    private final BoardHideRepository boardHideRepository;
    private final ReportRepository reportRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CommentNoticeEvent event) {
        // 게시물 알림 설정 체크
        if (!noticeService.isContentNoticeEnabled(event.recipientUserId(), event.contentId())) {
            return;
        }
        // 수신자가 발신자를 차단한 경우 → 생성/푸시 안 함
        if (userBlockRepository.existsByBlockerIdAndBlockedId(event.recipientUserId(), event.senderUserId())) {
            return;
        }
        // 수신자가 해당 게시글을 숨긴 경우 → 생성/푸시 안 함
        if (boardHideRepository.findByCreatedByIdAndContentId(event.recipientUserId(), event.contentId()).isPresent()) {
            return;
        }
        // 수신자가 해당 게시글을 신고한 경우 → 생성/푸시 안 함
        if (reportRepository.existsByCreatedByIdAndReportTypeAndTargetId(
                event.recipientUserId(), ReportType.BOARD, event.contentId())) {
            return;
        }
        fcmPushEventPublisher.publish(event.recipientUserId(), event.pushType(), event.pushData());
        noticeService.saveNotice(event.recipientUserId(), event.pushType(), event.pushData(),
                event.senderUserId(), event.refId());
    }
}
