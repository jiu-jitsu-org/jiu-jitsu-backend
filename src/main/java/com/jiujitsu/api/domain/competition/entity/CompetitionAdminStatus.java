package com.jiujitsu.api.domain.competition.entity;

/**
 * 관리자 공개 상태. 기존 published(boolean) 를 대체 예정
 * - DRAFT: 작성 중 (미공개)
 * - PUBLISHED: 앱에 공개
 * - UNPUBLISHED: 운영자가 비공개로 전환
 * - HIDDEN: 앱에서 숨김 (사용자에게 노출 안 함)
 */
public enum CompetitionAdminStatus {
    DRAFT,
    PUBLISHED,
    UNPUBLISHED,
    HIDDEN
}
