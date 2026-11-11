package com.jiujitsu.api.domain.competition.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대회 정보 제보 — 무엇이 다른지 (대회 상세 "정보가 달라요")
 */
@Getter
@RequiredArgsConstructor
public enum CompetitionInfoField {
    SCHEDULE("일시"),
    PLACE("장소"),
    REGISTRATION("접수 일정"),
    FEE("참가비"),
    OTHER("다른 정보");

    private final String description;
}
