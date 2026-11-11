package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionInfoField;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionInfoReportRequest(
        @NotNull(message = "무엇이 다른지는 필수입니다.")
        @Schema(description = "무엇이 다른지 (SCHEDULE 일시, PLACE 장소, REGISTRATION 접수 일정, FEE 참가비, OTHER 다른 정보)",
                example = "REGISTRATION", requiredMode = Schema.RequiredMode.REQUIRED)
        CompetitionInfoField field,
        @Size(max = CompetitionInfoReport.DETAIL_MAX_LENGTH, message = "내용은 200자 이하여야 합니다.")
        @Schema(description = "내용 (최대 200자). field 가 OTHER 면 필수, 나머지는 선택", example = "공지에는 접수 마감이 11월 3일이에요")
        String detail
) {
}
