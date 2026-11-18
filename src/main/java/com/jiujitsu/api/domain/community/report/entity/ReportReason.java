package com.jiujitsu.api.domain.community.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 신고 사유. 값을 더하면 PostgreSQL 의 report.reason CHECK 제약을 수동으로 고쳐야 한다 (ddl-auto: update 는 제약을 바꾸지 않음)
 */
@Getter
@RequiredArgsConstructor
public enum ReportReason {
    SPAM("광고 및 홍보"),
    ABUSE("욕설 및 비방"),
    ADULT("음란 또는 부적절한 콘텐츠"),
    HARASSMENT("혐오 및 괴롭힘"),
    INCITEMENT("분쟁 유도"),
    RIGHTS_VIOLATION("개인정보 노출 · 권리 침해"),
    ILLEGAL_FILMING("불법촬영물"),
    OTHER("기타");

    private final String description;
}
