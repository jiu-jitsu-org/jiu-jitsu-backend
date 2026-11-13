package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionAudience;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionRequiredField;
import com.jiujitsu.api.domain.competition.entity.CompetitionUniform;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 접수 상태·얼리버드 여부는 공개 대회만 있어 넣지 않는다 (미공개 대회도 열려야 함)
@Schema(description = "관리자 대회 상세 (입력 화면에 채울 전체 칸)")
public record AdminCompetitionDetailResponse(
        @Schema(description = "대회 ID", example = "1")
            Long competitionId,
        @Schema(description = "대회명", example = "2026 서울 오픈 주짓수 챔피언십")
            String name,
        @Schema(description = "개최 연도", example = "2026", nullable = true)
            Integer competitionYear,
        @Schema(description = "대회 탭 공개 여부")
            boolean published,
        @Schema(description = "대회일 (자동완성용 대회는 null)", example = "2026-11-22", nullable = true)
            LocalDate openDate,
        @JsonFormat(pattern = "HH:mm")   // 초는 쓰지 않음
        @Schema(description = "시작 시각 HH:mm", type = "string", example = "09:00", nullable = true)
            LocalTime startTime,
        @Schema(description = "지역(시·도)", nullable = true)
            CompetitionRegion region,
        @Schema(description = "짧은 장소 표기", example = "서울 송파", nullable = true)
            String place,
        @Schema(description = "경기장 이름", example = "올림픽홀", nullable = true)
            String venue,
        @Schema(description = "주최 단체", nullable = true)
            String host,
        @Schema(description = "대회 공지 링크", nullable = true)
            String noticeUrl,
        @Schema(description = "종목 (도복/노기)")
            List<CompetitionUniform> uniforms,
        @Schema(description = "참가 대상 제한 (제한 없으면 null)", nullable = true)
            CompetitionAudience audience,
        @Schema(description = "접수 시작일", nullable = true)
            LocalDate registrationStartDate,
        @Schema(description = "얼리버드 마감일", nullable = true)
            LocalDate earlyBirdDeadline,
        @Schema(description = "접수 마감일", nullable = true)
            LocalDate registrationDeadline,
        @Schema(description = "참가비 단계 (입력 순서). 모르면 빈 배열")
            List<CompetitionFeeResponse> fees,
        @Schema(description = "참가비 기준 문구", example = "성인 1종목 기준", nullable = true)
            String feeNote,
        @Schema(description = "정보 기준일(마지막 확인일)", nullable = true)
            LocalDate lastVerifiedDate,
        @Schema(description = "조회수", example = "214")
            long viewCount,
        @Schema(description = "관심 수", example = "32")
            long interestCount,
        @Schema(description = "공개에 필요한데 비어 있는 칸 (입력 화면 순서). 다 채워졌으면 빈 배열")
            List<CompetitionRequiredField> missingFields
) { }
