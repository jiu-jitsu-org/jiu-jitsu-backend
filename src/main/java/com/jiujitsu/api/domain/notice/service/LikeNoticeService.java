package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.board.repository.BoardHideRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.content.repository.ContentLikeRepository;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.repository.ReportRepository;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.event.LikeNoticeEvent;
import com.jiujitsu.api.domain.notice.factory.NoticeFactory;
import com.jiujitsu.api.domain.notice.repository.NoticeRepository;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import com.jiujitsu.api.global.properties.LikePushProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 좋아요 알림 (#196): 글·댓글마다 카드 한 장, 기준 개수(LikePushProperties)에 처음 닿을 때만 푸시
 * - 카드 숫자는 알림함 조회 때 실제 좋아요 수로 계산하므로 기준 사이 좋아요는 카드를 건드리지 않는다
 * - 취소 후 다시 눌러 같은 수가 돼도 이미 보낸 기준이라 다시 보내지 않는다
 * - 누가 눌렀는지 드러나지 않으므로 차단한 사람의 좋아요도 개수·푸시에 들어간다
 */
@Service
@RequiredArgsConstructor
public class LikeNoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeFactory noticeFactory;
    private final NoticeService noticeService;
    private final BoardHideRepository boardHideRepository;
    private final ReportRepository reportRepository;
    private final ContentLikeRepository contentLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NoticePreview noticePreview;
    private final FcmPushEventPublisher fcmPushEventPublisher;
    private final LikePushProperties likePushProperties;
    private final Clock clock;

    /**
     * 좋아요가 커밋된 뒤 호출. 푸시를 보냈으면 true
     * 처음 카드를 만들 때 동시에 들어온 다른 좋아요와 겹치면 카드 유니크 제약 위반으로 실패한다 — 호출 쪽이 한 번 더 부른다
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean handle(LikeNoticeEvent event) {
        Long recipientId = event.recipientUserId();
        Long contentId = event.contentId();

        // 게시물 알림을 껐거나, 게시글을 숨겼거나 신고했으면 카드도 푸시도 없음 (댓글 알림과 같은 기준)
        if (!noticeService.isContentNoticeEnabled(recipientId, contentId)) {
            return false;
        }
        if (boardHideRepository.findByCreatedByIdAndContentId(recipientId, contentId).isPresent()) {
            return false;
        }
        if (reportRepository.existsByCreatedByIdAndReportTypeAndTargetId(recipientId, ReportType.BOARD, contentId)) {
            return false;
        }

        Notice card = noticeRepository.findForUpdateByUserIdAndGroupKey(recipientId, event.groupKey())
                .orElseGet(() -> noticeRepository.saveAndFlush(noticeFactory.createLikeCard(event)));

        long count = countLikes(event);
        int threshold = likePushProperties.reachedThreshold(count);
        String title = Notice.likeTitle(event.pushType(), count);
        // 기준 사이 좋아요도 알림함 맨 위로 올린다 — 알림함은 가장 최근에 일어난 일이 위 (#215). 푸시는 새 기준에 닿을 때만
        if (threshold <= card.getLikePushedCount()) {
            card.touchLike(title, LocalDateTime.now(clock));
            return false;
        }

        card.pushLike(threshold, title, LocalDateTime.now(clock));
        fcmPushEventPublisher.publish(recipientId, event.pushType(), event.pushData(), title,
                noticePreview.of(event.pushType(), event.contentId(), event.commentId()));
        return true;
    }

    private long countLikes(LikeNoticeEvent event) {
        List<Object[]> rows = event.commentId() != null
                ? commentLikeRepository.countGroupByCommentIds(List.of(event.commentId()))
                : contentLikeRepository.countContentLikeByContentIds(List.of(event.contentId()));
        return rows.stream().findFirst().map(row -> ((Number) row[1]).longValue()).orElse(0L);
    }
}
