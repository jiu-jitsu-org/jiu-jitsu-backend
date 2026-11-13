package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "대회 정보 제보 처리 (관리자). 본문은 비워도 됨")
public record CompetitionInfoReportResolveRequest(
        @Size(max = CompetitionInfoReport.NOTE_MAX_LENGTH, message = "처리 메모는 100자 이하여야 합니다.")
        @Schema(description = "처리 메모 (선택, 100자) — 무엇을 했는지. 빼거나 null 이면 기존 메모 그대로, 지우려면 빈 문자열", example = "참가비 고침", nullable = true)
        String note
) { }
