package com.jiujitsu.api.domain.community.report.entity;

/**
 * 신고 대상 종류. targetId 기준: BOARD → content.id, COMMENT → comment.id, USER → user.id
 * 값을 더하면 PostgreSQL 의 report.report_type CHECK 제약을 수동으로 고쳐야 한다 (ddl-auto: update 는 제약을 바꾸지 않음)
 */
public enum ReportType {
    BOARD,
    COMMENT,
    USER
}
