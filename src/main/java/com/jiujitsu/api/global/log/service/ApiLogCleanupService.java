package com.jiujitsu.api.global.log.service;

import com.jiujitsu.api.global.log.repository.ApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 접속 로그(api_log) 보관 기간 초과분 자동 삭제 배치.
 *
 * <p>통신비밀보호법 기준 3개월(90일) 초과분을 매일 새벽 1시에 삭제합니다.
 * 보관 기간은 {@code api-log.retention-days} 설정으로 변경할 수 있습니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiLogCleanupService {

    private final ApiLogRepository apiLogRepository;

    @Value("${api-log.retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "0 0 1 * * ?") // 매일 새벽 1시
    @Transactional
    public void cleanupExpiredLogs() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        log.info("----- api_log 보관 기간 초과분 삭제 start (기준: {}일 이전, threshold={}) -----",
                retentionDays, threshold);

        int deleted = apiLogRepository.deleteByRequestedAtBefore(threshold);

        log.info("----- api_log 삭제 완료: {}건 -----", deleted);
    }
}
