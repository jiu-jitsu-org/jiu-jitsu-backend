package com.jiujitsu.api.domain.competition.entity;

/**
 * 대회 접수/진행 상태. 저장하지 않고 대회일·종료일·접수 일정과 오늘 날짜로 파생한다
 * (Competition#status 참고). 시간이 지나면 상태가 바뀌므로 컬럼으로 두면 어긋난다.
 * "대회 중"은 따로 두지 않는다 — 여러 날 대회의 둘째 날부터 마지막 날까지는 CLOSED, 앱은 대회일·종료일·today 로 대회 중을 판단한다 (#223)
 */
public enum CompetitionStatus {
    UPCOMING,   // 접수 예정 (접수 시작 전)
    OPEN,       // 접수 중
    CLOSED,     // 접수 마감 (대회 전·대회 당일, 여러 날 대회는 마지막 날까지)
    ENDED       // 대회 종료 (마지막 날 다음 날부터)
}
