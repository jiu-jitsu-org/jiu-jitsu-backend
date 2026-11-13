package com.jiujitsu.api.domain.competition.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * 관리자가 입력하는 대회 정보 칸 묶음 (Competition#changeEventInfo). 대회명·연도·공개 여부·마지막 확인일은 따로 바꾼다
 * 비어 있는 칸은 null, 종목·참가비는 빈 컬렉션
 */
public record CompetitionEventInfo(
        LocalDate openDate,
        LocalDate endDate,
        LocalTime startTime,
        CompetitionRegion region,
        String place,
        String venue,
        String host,
        String noticeUrl,
        Set<CompetitionUniform> uniforms,
        CompetitionAudience audience,
        LocalDate registrationStartDate,
        LocalDate earlyBirdDeadline,
        LocalDate registrationDeadline,
        List<CompetitionFee> fees,
        String feeNote
) { }
