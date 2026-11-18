package com.jiujitsu.api.domain.user.entity;

/**
 * 분석에서 일반 회원과 나눠 보기 위한 계정 구분. 권한({@link UserRole})과는 무관하다.
 */
public enum AccountType {
    MEMBER,     // 일반 회원
    OPERATOR,   // OSS 공식 · 운영자 계정
    INTERNAL    // 팀원 · 심사용 · 테스트 계정
}
