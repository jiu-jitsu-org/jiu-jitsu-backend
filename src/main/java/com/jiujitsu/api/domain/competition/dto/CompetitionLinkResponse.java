package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionLinkType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "대회 상태 링크 — 지금 보이는 것만 (운영자가 승인했고 보이는 기간 안)")
public record CompetitionLinkResponse(
        @Schema(description = "종류. REGISTRATION 신청 · ENTRY_LIST 참가자 현황 · BRACKET 대진표·시간표 · LIVE_MAT 실시간 매트 · RESULT 결과")
            CompetitionLinkType type,
        @Schema(description = "접수 사이트 페이지 주소", example = "https://flowcomp.co.kr/...")
            String url,
        @Schema(description = "보이기 시작 (한국 시간, 포함)", example = "2026-11-01T00:00:00")
            LocalDateTime visibleFrom,
        @Schema(description = "보이는 끝 (한국 시간, 포함 — 이 초가 끝날 때까지). 끝이 없으면 null (결과 주소가 고정인 사이트의 결과)", example = "2026-11-22T23:59:59", nullable = true)
            LocalDateTime visibleUntil
) { }
