package com.jiujitsu.api.domain.competition.entity;

/**
 * 대회 링크 확인 필요 사유
 */
public enum CompetitionLinkCheckNeededReason {
    DEADLINE_TEXT,
    DIFFERENT_ROUND,
    NOT_FOUND,
    READ_TIMEOUT
}
