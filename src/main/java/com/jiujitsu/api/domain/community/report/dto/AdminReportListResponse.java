package com.jiujitsu.api.domain.community.report.dto;

import com.jiujitsu.api.domain.community.report.entity.ReportReason;
import com.jiujitsu.api.domain.community.report.entity.ReportType;

import java.time.LocalDateTime;

public record AdminReportListResponse(
        Long reportId,
        ReportType reportType,
        Long targetId,
        String targetPreview,
        Long reporterUserId,
        String reporterNickname,
        ReportReason reason,
        String detail,
        long totalReportCount,
        boolean isHidden,
        LocalDateTime createdAt
) {
}
