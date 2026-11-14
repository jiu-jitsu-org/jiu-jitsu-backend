package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionAudience;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionStatus;
import com.jiujitsu.api.domain.competition.entity.CompetitionUniform;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "대회 상세")
public record CompetitionDetailResponse(
        @Schema(description = "서버 기준 오늘. status·earlyBirdOpen 판단 기준이자 남은 날 계산 기준", example = "2026-11-09")
            LocalDate today,
        @Schema(description = "대회 ID (결과 남기기에서 프로필 대회 기록 pre-fill 에 사용)", example = "1")
            Long competitionId,
        @Schema(description = "대회명", example = "2026 서울 오픈 주짓수 챔피언십")
            String name,
        @Schema(description = "대회일 (여러 날 대회면 첫날)", example = "2026-11-22")
            LocalDate openDate,
        @Schema(description = "여러 날 대회의 마지막 날. 하루짜리면 null (목록과 같음)", example = "2026-11-23", nullable = true)
            LocalDate endDate,
        @JsonFormat(pattern = "HH:mm")   // 초는 쓰지 않음
        @Schema(description = "시작 시각 HH:mm (모르면 null)", type = "string", example = "09:00", nullable = true)
            LocalTime startTime,
        @Schema(description = "지역(시·도)", nullable = true)
            CompetitionRegion region,
        @Schema(description = "짧은 장소 표기 (목록과 같음)", example = "서울 송파", nullable = true)
            String place,
        @Schema(description = "경기장 이름", example = "올림픽홀", nullable = true)
            String venue,
        @Schema(description = "종목 (도복/노기)")
            List<CompetitionUniform> uniforms,
        @Schema(description = "참가 대상 제한 (제한 없으면 null)", nullable = true)
            CompetitionAudience audience,
        @Schema(description = "주최 단체", nullable = true)
            String host,
        @Schema(description = "대회 공지 링크", nullable = true)
            String noticeUrl,
        @Schema(description = "오늘 기준 접수/진행 상태. 마지막 날 다음 날부터 ENDED, 여러 날 대회의 둘째 날부터 마지막 날까지는 CLOSED")
            CompetitionStatus status,
        @Schema(description = "오늘 기준 얼리버드 접수 중인지. 어느 마감을 보여 주고 강조할지(예: D-7부터)는 앱 화면 규칙 — 날짜 원본과 today 로 계산")
            boolean earlyBirdOpen,
        @Schema(description = "접수 시작일", nullable = true)
            LocalDate registrationStartDate,
        @Schema(description = "얼리버드 마감일", nullable = true)
            LocalDate earlyBirdDeadline,
        @Schema(description = "접수 마감일", nullable = true)
            LocalDate registrationDeadline,
        @Schema(description = "참가비 단계 (입력 순서). 확인되지 않았으면 빈 배열")
            List<CompetitionFeeResponse> fees,
        @Schema(description = "참가비 기준 문구", example = "성인 1종목 기준", nullable = true)
            String feeNote,
        @Schema(description = "정보 기준일(마지막 확인일)", nullable = true)
            LocalDate lastVerifiedDate,
        @Schema(description = "조회수 (이 조회로는 오르지 않음. POST /competitions/{id}/views)", example = "214")
            long viewCount,
        @Schema(description = "관심 수 (익명 집계)", example = "32")
            long interestCount,
        @Schema(description = "내 관심 여부 (비로그인은 false)")
            boolean interested,
        @Schema(description = "이 대회 알림 켜짐 (관심 등록 + 이 대회 알림 켜짐. 비로그인·관심 없음은 false). 실제로 알림이 오려면 설정의 관심 대회 알림(GET /notice/setting 의 competitionEnabled)도 켜져 있어야 함 — 앱은 두 값을 함께 본다")
            boolean noticeEnabled
) { }
