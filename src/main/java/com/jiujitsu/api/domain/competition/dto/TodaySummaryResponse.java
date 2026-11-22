package com.jiujitsu.api.domain.competition.dto;

import java.time.LocalDateTime;

/**
 * 오늘 할 일 화면 헤더 — 수집·알림 요약
 */
public record TodaySummaryResponse(
        LocalDateTime lastCrawlAt,
        int successSiteCount,
        int failureSiteCount,
        LocalDateTime nextCrawlAt,
        long todayNotificationCount,   // 오늘 18:00 마감 알림 대상 수
        long pendingPreCheckCount      // 알림 전 확인 남은 수
) {}
