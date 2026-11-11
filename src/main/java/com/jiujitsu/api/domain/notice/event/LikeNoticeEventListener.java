package com.jiujitsu.api.domain.notice.event;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.service.LikeNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;

/**
 * 좋아요 커밋 뒤 알림 처리 (#196). 실제 좋아요 수를 세려면 좋아요가 커밋된 뒤여야 하고,
 * 알림이 실패해도 좋아요 응답은 그대로 성공해야 하므로 예외는 로그만 남긴다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeNoticeEventListener {

    private final LikeNoticeService likeNoticeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LikeNoticeEvent event) {
        try {
            likeNoticeService.handle(event);
        } catch (RuntimeException e) {
            if (!isCardDuplicate(e)) {
                log.warn("좋아요 알림 실패 recipient={} key={}", event.recipientUserId(), event.groupKey(), e);
                return;
            }
            // 같은 카드를 동시에 처음 만들다 겹침 → 이제 카드가 있으니 한 번 더 (잠금 조회로 이어서 처리)
            try {
                likeNoticeService.handle(event);
            } catch (RuntimeException retry) {
                log.warn("좋아요 알림 재시도 실패 recipient={} key={}", event.recipientUserId(), event.groupKey(), retry);
            }
        }
    }

    // 좋아요 카드 유니크 제약 위반만 겹침으로 본다 (H2 는 제약 이름을 대문자로 적어 대소문자 무시)
    public static boolean isCardDuplicate(RuntimeException e) {
        if (!(e instanceof DataIntegrityViolationException)) {
            return false;
        }
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(Notice.GROUP_KEY_UNIQUE_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }
}
