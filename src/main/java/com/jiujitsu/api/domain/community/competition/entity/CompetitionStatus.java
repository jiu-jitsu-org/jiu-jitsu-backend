package com.jiujitsu.api.domain.community.competition.entity;

/**
 * 대회 접수/진행 상태. 저장하지 않고 개최일·접수 일정과 오늘 날짜로 파생한다
 * (Competition#status 참고). 시간이 지나면 상태가 바뀌므로 컬럼으로 두면 어긋난다.
 */
public enum CompetitionStatus {
    UPCOMING,   // 접수 예정 (접수 시작 전)
    OPEN,       // 접수 중
    CLOSED,     // 접수 마감 (대회 전)
    ENDED       // 대회 종료
}
