package com.jiujitsu.api.domain.competition.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대회 접수 사이트 (#221). 상태 링크 규칙이 사이트에 따라 다르다
 * - permanentResult: 결과 주소가 대회 번호로 고정이라 끝나고 14일 뒤에도 결과 링크를 보여도 됨 (스트릿은 지난 회차 페이지를 다음 회차에 다시 씀)
 * - clubRegistrationOnly: 감독 · 코치만 신청할 수 있어 신청 링크를 두지 않고 "도장을 통해 신청" 안내 (대한체육회)
 * 값을 추가할 때: PostgreSQL 은 enum 컬럼을 CHECK 제약으로 만들고 ddl-auto update 는 제약을 고치지 않으므로 dev DB 의 check 제약도 함께 갱신해야 한다
 */
@Getter
@RequiredArgsConstructor
public enum RegistrationSite {
    FLOWCOMP("FLOWCOMP", true, false),
    STREET_JIUJITSU("스트릿 주짓수", false, false),
    JIUDAEPL("주대플", true, false),
    KSOC("대한체육회", true, true),
    OTHER("기타", false, false);

    private final String description;
    private final boolean permanentResult;
    private final boolean clubRegistrationOnly;
}
