package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionAudience;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionStatus;
import com.jiujitsu.api.domain.competition.entity.CompetitionUniform;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "대회 목록 항목")
public record CompetitionListItem(
        @Schema(description = "대회 ID", example = "1")
            Long competitionId,
        @Schema(description = "대회명", example = "2026 서울 오픈 주짓수 챔피언십")
            String name,
        @Schema(description = "대회일", example = "2026-12-06")
            LocalDate openDate,
        @Schema(description = "지역(시·도)", nullable = true)
            CompetitionRegion region,
        @Schema(description = "장소", example = "서울 송파 올림픽홀", nullable = true)
            String place,
        @Schema(description = "종목 (도복/노기)")
            List<CompetitionUniform> uniforms,
        @Schema(description = "참가 대상 제한 (제한 없으면 null)", nullable = true)
            CompetitionAudience audience,
        @Schema(description = "오늘(응답 today) 기준 접수/진행 상태")
            CompetitionStatus status,
        @Schema(description = "오늘 기준 얼리버드 접수 중인지")
            boolean earlyBirdOpen,
        @Schema(description = "접수 시작일", nullable = true)
            LocalDate registrationStartDate,
        @Schema(description = "얼리버드 마감일", nullable = true)
            LocalDate earlyBirdDeadline,
        @Schema(description = "접수 마감일", nullable = true)
            LocalDate registrationDeadline,
        @Schema(description = "조회수", example = "214")
            long viewCount,
        @Schema(description = "관심 수 (익명 집계)", example = "12")
            long interestCount,
        @Schema(description = "내 관심 여부 (비로그인은 false)")
            boolean interested,
        @Schema(description = "관심 많은 대회 — 같은 달(대회일 기준) 끝나지 않은 공개 대회 중 관심 수 상위 3개, 단 관심 5개 이상 (필터와 무관). 끝난 대회는 false")
            boolean popular
) { }
