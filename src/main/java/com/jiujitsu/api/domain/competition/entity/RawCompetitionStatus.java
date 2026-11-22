package com.jiujitsu.api.domain.competition.entity;

/**
 * 크롤러 신규 대회 후보 처리 상태
 * - PENDING: 처리 대기 중 (오늘 할 일에 표시)
 * - LINKED: 기존 등록 대회에 출처 연결 완료
 * - DRAFT_CREATED: 새 대회로 만들기 선택 후 공개 전 저장 (입력 중)
 * - PUBLISHED: 앱에 공개 완료
 * - EXCLUDED: 대회 아님으로 제외
 */
public enum RawCompetitionStatus {
    PENDING,
    LINKED,
    DRAFT_CREATED,
    PUBLISHED,
    EXCLUDED
}
