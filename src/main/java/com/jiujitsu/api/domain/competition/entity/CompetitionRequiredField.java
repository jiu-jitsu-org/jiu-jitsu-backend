package com.jiujitsu.api.domain.competition.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대회 탭에 공개하려면 채워야 하는 칸 (Competition#missingRequiredFields). 선언 순서 = 관리자 입력 화면 순서
 * 대회명은 NOT NULL 이고 등록 시 검사해 비는 경우가 없어 넣지 않는다
 * 마지막 확인일도 넣지 않는다 — 공개하면 오늘로 채운다 (공개 = 공지와 맞춰 본 대회, #209). 그래서 빈 칸이 없으면 공개할 수 있다
 * 저장하지 않는 값이라 DB CHECK 제약과 무관하다
 */
@Getter
@RequiredArgsConstructor
public enum CompetitionRequiredField {
    OPEN_DATE("대회일"),
    START_TIME("시작 시각"),
    REGION("지역"),
    UNIFORMS("종목"),
    REGISTRATION_START_DATE("접수 시작"),
    REGISTRATION_DEADLINE("접수 마감"),
    NOTICE_URL("대회 공지 링크");

    private final String description;
}
