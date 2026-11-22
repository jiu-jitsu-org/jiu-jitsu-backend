package com.jiujitsu.api.domain.competition.dto;

/**
 * 오늘 할 일 종류
 */
public enum TodayTaskType {
    /** 사이트 수집 실패 */
    CRAWL_FAILURE,
    /** 알림 전 확인 (내일 마감인데 오늘 크롤러가 확인 못 한 경우) */
    PRE_CHECK,
    /** 새 대회 (크롤러가 발견, 아직 등록 안 된 원문) */
    NEW_COMPETITION,
    /** 바뀐 값 (크롤러 값 ≠ 현재 값) */
    CHANGED_VALUE,
    /** 원문에서 사라짐 */
    DISAPPEARED,
    /** 새 링크 (승인 전 — 처음 발견) */
    NEW_LINK,
    /** 새 주소 후보 (승인된 링크와 다른 주소 발견) */
    NEW_URL,
    /** 링크 확인 (크롤러가 이상 감지) */
    LINK_CHECK,
    /** 사용자 제보 */
    INFO_REPORT,
    /** 마감 알림 발송 실패 */
    NOTIFICATION_FAILURE
}
