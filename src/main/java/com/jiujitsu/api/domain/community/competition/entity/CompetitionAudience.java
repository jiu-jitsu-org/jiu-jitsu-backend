package com.jiujitsu.api.domain.community.competition.entity;

/**
 * 대회 참가 대상 제한. null = 제한 없음(전체).
 * 목록 카드에서 대상이 제한된 대회(예: 키즈)만 대상 표시.
 */
public enum CompetitionAudience {
    ADULT,  // 성인부
    KIDS,   // 키즈·유소년부
    ALL     // 전 연령(명시적으로 전체를 표기해야 할 때)
}
