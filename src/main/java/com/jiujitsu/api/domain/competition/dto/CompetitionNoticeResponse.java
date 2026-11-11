package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이 대회 알림 켜기·끄기 결과")
public record CompetitionNoticeResponse(
        @Schema(description = "대회 ID", example = "1")
        Long competitionId,
        @Schema(description = "처리 후 내 관심 상태 (켜기는 관심도 함께 등록)", example = "true")
        boolean interested,
        @Schema(description = "처리 후 관심 수 (익명)", example = "12")
        long interestCount,
        @Schema(description = "처리 후 이 대회 마감 알림 상태", example = "true")
        boolean noticeEnabled
) {
}
