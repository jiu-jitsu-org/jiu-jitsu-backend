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

    private static final int RETENTION_DAYS = 30;

    private final NoticeRepository noticeRepository;

    /**
     * 30일 경과 알림 자동 삭제 (매일 새벽 2시)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void deleteExpiredNotices() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        noticeRepository.deleteByCreatedAtBefore(threshold);
    }
}
