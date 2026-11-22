package com.jiujitsu.api.domain.competition.entity;

/**
 * 크롤러 발견 항목을 "대회 아님"으로 제외한 사유
 */
public enum ExcludedReason {
    NOT_COMPETITION,
    DUPLICATE_SITE
}
