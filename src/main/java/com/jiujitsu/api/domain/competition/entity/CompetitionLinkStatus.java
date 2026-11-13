package com.jiujitsu.api.domain.competition.entity;

/**
 * 상태 링크 확인 상태 (#221). 운영자가 승인한 링크만 앱에 나간다
 * 값을 추가할 때: dev DB 의 competition_links check 제약도 함께 갱신 (RegistrationSite 참고)
 */
public enum CompetitionLinkStatus {
    PENDING,    // 승인 전 — 새로 넣었거나 주소가 바뀜
    APPROVED,   // 승인 — 보이는 기간 안이면 앱에 나감
    HIDDEN      // 숨김 — 조기 마감 · 틀린 페이지 등으로 운영자가 내림
}
