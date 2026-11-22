package com.jiujitsu.api.domain.competition.entity;

/**
 * 마감 알림 발송 상태
 */
public enum DeadlineNotificationStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED
}
