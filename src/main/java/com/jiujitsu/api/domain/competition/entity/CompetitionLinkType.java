package com.jiujitsu.api.domain.competition.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대회 상태 링크 종류 (#221) — 대회 상태에 맞는 접수 사이트 페이지. 보이는 기간은 CompetitionLinkWindow
 * 값을 추가할 때: dev DB 의 competition_links check 제약도 함께 갱신 (RegistrationSite 참고)
 */
@Getter
@RequiredArgsConstructor
public enum CompetitionLinkType {
    REGISTRATION("신청"),
    ENTRY_LIST("참가자 현황"),
    BRACKET("대진표 · 시간표"),
    LIVE_MAT("실시간 매트"),
    RESULT("결과");

    private final String description;
}
