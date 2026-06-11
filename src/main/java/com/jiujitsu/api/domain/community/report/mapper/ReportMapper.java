package com.jiujitsu.api.domain.community.report.mapper;

import com.jiujitsu.api.domain.community.report.dto.AdminReportListResponse;
import com.jiujitsu.api.domain.community.report.entity.Report;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public AdminReportListResponse toAdminListResponse(Report report,
                                                       String targetPreview,
                                                       long totalReportCount,
                                                       boolean isHidden) {
        return new AdminReportListResponse(
                report.getId(),
                report.getReportType(),
                report.getTargetId(),
                truncate(targetPreview, 100),
                report.getCreatedBy().getId(),
                report.getCreatedBy().getNickname(),
                report.getReason(),
                totalReportCount,
                isHidden,
                report.getCreatedAt()
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
