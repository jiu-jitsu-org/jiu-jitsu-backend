package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionRequiredField;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "관리자 대회 목록 한 줄")
public record AdminCompetitionListItem(
        @Schema(description = "대회 ID", example = "1")
            Long competitionId,
        @Schema(description = "대회명", example = "2026 서울 오픈 주짓수 챔피언십")
            String name,
        @Schema(description = "개최 연도", example = "2026", nullable = true)
            Integer competitionYear,
        @Schema(description = "대회 탭 공개 여부")
            boolean published,
        @Schema(description = "대회일", example = "2026-11-22")
            LocalDate openDate,
        @Schema(description = "지역(시·도)", nullable = true)
            CompetitionRegion region,
        @Schema(description = "짧은 장소 표기", example = "서울 송파", nullable = true)
            String place,
        @Schema(description = "접수 마감일", nullable = true)
            LocalDate registrationDeadline,
        @Schema(description = "정보 기준일(마지막 확인일)", nullable = true)
            LocalDate lastVerifiedDate,
        @Schema(description = "공개에 필요한데 비어 있는 칸 (입력 화면 순서). 빈 배열이면 공개할 수 있음 (마지막 확인일은 공개할 때 오늘로 채워져 넣지 않음)")
            List<CompetitionRequiredField> missingFields
) { }
