package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoticeCleanupService {

    // 알림함 보관 기간. 좋아요 카드는 지우지 않고 알림함에서만 이 기간 뒤 숨긴다 (NoticeService)
    public static final int RETENTION_DAYS = 30;

    private final NoticeRepository noticeRepository;

    /**
     * 30일 경과 알림 자동 삭제 (매일 새벽 2시). 좋아요 카드는 이미 보낸 기준을 잃지 않게 남긴다 (#196)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void deleteExpiredNotices() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        noticeRepository.deleteExpiredExceptLikeCards(threshold);
    }
}
