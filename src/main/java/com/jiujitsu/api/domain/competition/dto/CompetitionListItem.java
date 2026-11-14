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
        @Schema(description = "대회일 (여러 날 대회면 첫날)", example = "2026-12-06")
            LocalDate openDate,
        @Schema(description = "여러 날 대회의 마지막 날. 하루짜리면 null. 끝남(status ENDED)·관심 많은 대회의 \"끝나지 않은 대회\"는 마지막 날(이 값, 없으면 대회일) 기준", example = "2026-12-07", nullable = true)
            LocalDate endDate,
        @Schema(description = "지역(시·도)", nullable = true)
            CompetitionRegion region,
        @Schema(description = "장소", example = "서울 송파 올림픽홀", nullable = true)
            String place,
        @Schema(description = "종목 (도복/노기)")
            List<CompetitionUniform> uniforms,
        @Schema(description = "참가 대상 제한 (제한 없으면 null)", nullable = true)
            CompetitionAudience audience,
        @Schema(description = "오늘(응답 today) 기준 접수/진행 상태. 마지막 날 다음 날부터 ENDED, 여러 날 대회의 둘째 날부터 마지막 날까지는 CLOSED")
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
        @Schema(description = "관심 많은 대회 — 같은 달(대회일 기준) 끝나지 않은(마지막 날이 오늘 이후) 공개 대회 중 관심 수 상위 N개, 단 관심 M개 이상 (필터와 무관, 동점은 대회일 빠른 순). N·M은 서버 설정 competition.popular.top·min-interests (현재 3·5). 끝난 대회는 false")
            boolean popular
) { }
