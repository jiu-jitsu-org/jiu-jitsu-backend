package com.jiujitsu.api.domain.community.report.dto;

import com.jiujitsu.api.domain.community.report.entity.ReportReason;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReportCreateRequest(
        @NotNull(message = "신고 컨텐츠 구분은 필수입니다.")
        @Schema(description = "신고 컨텐츠 구분", example = "BOARD", requiredMode = Schema.RequiredMode.REQUIRED)
        ReportType reportType,
        @NotNull(message = "신고 컨텐츠 id는 필수입니다.")
        @Schema(description = "컨텐츠 id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetId,
        @NotNull(message = "신고 사유는 필수입니다.")
        @Schema(description = "신고 사유", example = "SPAM", requiredMode = Schema.RequiredMode.REQUIRED)
        ReportReason reason
) {
}
