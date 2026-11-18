package com.jiujitsu.api.domain.community.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
