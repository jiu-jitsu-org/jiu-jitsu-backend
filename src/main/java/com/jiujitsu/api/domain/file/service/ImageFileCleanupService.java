package com.jiujitsu.api.domain.file.service;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.ImageFileStatus;
import com.jiujitsu.api.domain.file.event.ImageFileDeletedEvent;
import com.jiujitsu.api.domain.file.repository.ImageFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TEMP 상태 이미지 주기 정리 배치.
 *
 * <p>선택 즉시 업로드 방식에서 발생하는 미아 이미지(작성 취소·강제 종료·크롭 교체 등)를
 * 매일 새벽 3시에 삭제합니다.
 * 보관 기간은 {@value RETENTION_HOURS}시간으로, 작성 화면을 하루 이상 방치하는 경우를 커버합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageFileCleanupService {

    private static final int RETENTION_HOURS = 24;

    private final ImageFileRepository imageFileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시
    @Transactional
    public void cleanupExpiredTempImages() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(RETENTION_HOURS);
        List<ImageFile> expired = imageFileRepository.findByStatusAndUpdatedAtBefore(
                ImageFileStatus.TEMP, threshold);

        log.info("----- TEMP 이미지 정리 start (기준: {}시간 이전, 대상: {}건) -----",
                RETENTION_HOURS, expired.size());

        expired.forEach(image -> {
            if (image.getCdnId() != null) {
                eventPublisher.publishEvent(new ImageFileDeletedEvent(image.getCdnId()));
            }
        });

        imageFileRepository.deleteAll(expired);

        log.info("----- TEMP 이미지 정리 완료: {}건 삭제 -----", expired.size());
    }
}
