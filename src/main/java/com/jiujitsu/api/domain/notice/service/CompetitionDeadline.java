package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionFee;
import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineType;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 어떤 날 끝나는 마감 하나 (마감 전날 알림 한 건의 단위)
 * label 은 알림 문구에 쓰는 마감 이름 — 할인 단계 이름(1차 얼리버드 등), 단계가 없으면 "얼리버드", 접수는 "접수"
 */
public record CompetitionDeadline(CompetitionDeadlineType type, String label, LocalDate date) {

    private static final String EARLY_BIRD_LABEL = "얼리버드";
    private static final String GUIDE = "신청과 결제는 대회 공지에서";

    /**
     * 이 대회에서 date 에 끝나는 마감들
     * - 접수 마감이 그날이면 접수 마감 하나만 (같은 날 끝나는 얼리버드는 접수 마감 알림으로 충분)
     * - 아니면 그날 끝나는 할인 단계 (until 이 있는 참가비 단계, 단계마다 가격이 오른다)
     * - 단계 정보 없이 얼리버드 마감만 아는 대회는 "얼리버드"
     * 할인 단계 until 은 오름차순·중복 없음이라 같은 날 단계는 하나뿐이다 (Competition#validateFees)
     */
    public static List<CompetitionDeadline> dueOn(Competition competition, LocalDate date) {
        if (date.equals(competition.getRegistrationDeadline())) {
            return List.of(new CompetitionDeadline(CompetitionDeadlineType.REGISTRATION, "접수", date));
        }
        List<CompetitionDeadline> tiers = competition.getFees().stream()
                .filter(fee -> date.equals(fee.getUntil()))
                .map(CompetitionFee::getLabel)
                .filter(Objects::nonNull)
                .map(label -> new CompetitionDeadline(CompetitionDeadlineType.EARLY_BIRD, label, date))
                .toList();
        if (!tiers.isEmpty()) {
            return tiers;
        }
        if (date.equals(competition.getEarlyBirdDeadline())) {
            return List.of(new CompetitionDeadline(CompetitionDeadlineType.EARLY_BIRD, EARLY_BIRD_LABEL, date));
        }
        return List.of();
    }

    // 예: "1차 얼리버드 마감 내일", "접수 마감 내일"
    public String title() {
        return label + " 마감 내일";
    }

    // 예: "서울 오픈 1차 얼리버드 마감이 내일이에요. 신청과 결제는 대회 공지에서"
    public String body(String competitionName) {
        return type == CompetitionDeadlineType.REGISTRATION
                ? competitionName + " 접수가 내일 끝나요. " + GUIDE
                : competitionName + " " + label + " 마감이 내일이에요. " + GUIDE;
    }
}
