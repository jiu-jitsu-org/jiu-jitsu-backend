package com.jiujitsu.api.domain.competition.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 상태 링크가 보이는 기간 (#221, 대회 접수 페이지 연결 결정). 한국 시간, 마지막 날 = Competition#lastDay
 * | 종류 | 시작 | 끝 |
 * | 신청 | 접수 시작일 00:00, 없으면 승인 시각 | 접수 마감일 23:59:59, 없으면 대회 전날 23:59:59 |
 * | 참가자 현황 · 대진표 | 승인 시각 | 마지막 날 23:59:59 |
 * | 실시간 매트 | 대회일 00:00 | 마지막 날 23:59:59 |
 * | 결과 | 마지막 날 다음 날 00:00 | 마지막 날 + 14일 23:59:59, 결과 주소가 고정인 사이트는 끝 없음 |
 * 보임 = 승인 상태 + 기간 안. 승인 시각에서 시작하는 종류는 승인 전이면 시작이 없다(null)
 *
 * @param from  보이기 시작 (포함). 아직 정해지지 않았으면 null
 * @param until 보이는 끝 (포함). 끝이 없으면 null
 */
public record CompetitionLinkWindow(LocalDateTime from, LocalDateTime until) {

    public static final int RESULT_DAYS = 14;
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    public static CompetitionLinkWindow of(CompetitionLink link, Competition competition) {
        LocalDate openDate = competition.getOpenDate();
        LocalDate lastDay = competition.lastDay();
        if (openDate == null) {
            return new CompetitionLinkWindow(null, null);   // 자동완성용 대회 — 링크를 둘 수 없음 (서비스가 막음)
        }
        LocalDateTime approvedAt = link.getApprovedAt();
        return switch (link.getType()) {
            case REGISTRATION -> new CompetitionLinkWindow(
                    competition.getRegistrationStartDate() != null ? competition.getRegistrationStartDate().atStartOfDay() : approvedAt,
                    (competition.getRegistrationDeadline() != null ? competition.getRegistrationDeadline() : openDate.minusDays(1)).atTime(END_OF_DAY));
            case ENTRY_LIST, BRACKET -> new CompetitionLinkWindow(approvedAt, lastDay.atTime(END_OF_DAY));
            case LIVE_MAT -> new CompetitionLinkWindow(openDate.atStartOfDay(), lastDay.atTime(END_OF_DAY));
            case RESULT -> new CompetitionLinkWindow(
                    lastDay.plusDays(1).atStartOfDay(),
                    resultIsPermanent(competition) ? null : lastDay.plusDays(RESULT_DAYS).atTime(END_OF_DAY));
        };
    }

    /**
     * 지금 앱에 보이는가 — 승인 + 기간 안 (양 끝 포함)
     */
    public static boolean isVisible(CompetitionLink link, Competition competition, LocalDateTime now) {
        if (link.getStatus() != CompetitionLinkStatus.APPROVED) {
            return false;
        }
        return of(link, competition).contains(now);
    }

    public boolean contains(LocalDateTime now) {
        return from != null && !now.isBefore(from) && (until == null || !now.isAfter(until));
    }

    private static boolean resultIsPermanent(Competition competition) {
        return competition.getRegistrationSite() != null && competition.getRegistrationSite().isPermanentResult();
    }
}
