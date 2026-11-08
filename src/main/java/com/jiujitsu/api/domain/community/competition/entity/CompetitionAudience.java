package com.jiujitsu.api.domain.community.competition.entity;

/**
 * 대회 참가 대상 제한. 제한이 없으면 null (전체를 뜻하는 값을 따로 두지 않는다 — 같은 의미를 두 가지로 표현하지 않기 위해).
 * 목록 카드에서 대상이 제한된 대회(예: 키즈)만 대상 표시.
 * 값을 추가할 때: PostgreSQL 은 enum 컬럼을 CHECK 제약으로 만들고 ddl-auto update 는 제약을 고치지 않으므로
 * dev DB 의 check 제약도 함께 갱신해야 한다.
 */
public enum CompetitionAudience {
    ADULT,  // 성인부
    KIDS    // 키즈·유소년부
}
