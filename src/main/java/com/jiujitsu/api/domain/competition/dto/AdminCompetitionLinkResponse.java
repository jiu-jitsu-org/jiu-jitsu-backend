package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionLinkStatus;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 대회 상태 링크")
public record AdminCompetitionLinkResponse(
        @Schema(description = "종류")
            CompetitionLinkType type,
        @Schema(description = "주소")
            String url,
        @Schema(description = "PENDING 승인 전 · APPROVED 승인 · HIDDEN 숨김. 승인한 링크만 앱에 나감")
            CompetitionLinkStatus status,
        @Schema(description = "처음 승인한 시각 (한국 시간)", nullable = true)
            LocalDateTime approvedAt,
        @Schema(description = "보이기 시작 (한국 시간, 포함). 승인 시각에서 시작하는 종류는 승인 전이면 null", nullable = true)
            LocalDateTime visibleFrom,
        @Schema(description = "보이는 끝 (한국 시간, 포함). 끝이 없으면 null (결과 주소가 고정인 사이트의 결과)", nullable = true)
            LocalDateTime visibleUntil,
        @Schema(description = "지금 앱에 보이는가 (승인 + 기간 안)")
            boolean visibleNow
) { }
