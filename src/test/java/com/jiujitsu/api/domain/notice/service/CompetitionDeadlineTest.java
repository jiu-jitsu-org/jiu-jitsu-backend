package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionFee;
import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompetitionDeadlineTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 20);

    private static Competition competition(LocalDate earlyBird, LocalDate registration, CompetitionFee... fees) {
        Competition competition = Competition.builder()
                .name("서울 오픈")
                .published(true)
                .earlyBirdDeadline(earlyBird)
                .registrationDeadline(registration)
                .build();
        ReflectionTestUtils.setField(competition, "fees", new ArrayList<>(List.of(fees)));
        return competition;
    }

    private static CompetitionFee fee(String label, LocalDate until) {
        return new CompetitionFee(label, 50000, until);
    }

    @Test
    @DisplayName("접수 마감이 그날이면 접수 마감 하나만 (같은 날 끝나는 얼리버드는 보내지 않음)")
    void registration_only() {
        List<CompetitionDeadline> due = CompetitionDeadline.dueOn(
                competition(D, D, fee("얼리버드", D), fee("기본", null)), D);

        assertThat(due).containsExactly(new CompetitionDeadline(CompetitionDeadlineType.REGISTRATION, "접수", D));
    }

    @Test
    @DisplayName("할인 단계가 여럿이면 그날 끝나는 단계 하나 (단계 이름으로)")
    void tier_onThatDay() {
        Competition competition = competition(D.plusDays(10), D.plusDays(20),
                fee("1차 얼리버드", D), fee("2차 얼리버드", D.plusDays(10)), fee("기본 참가비", null));

        assertThat(CompetitionDeadline.dueOn(competition, D))
                .containsExactly(new CompetitionDeadline(CompetitionDeadlineType.EARLY_BIRD, "1차 얼리버드", D));
        assertThat(CompetitionDeadline.dueOn(competition, D.plusDays(10)))
                .containsExactly(new CompetitionDeadline(CompetitionDeadlineType.EARLY_BIRD, "2차 얼리버드", D.plusDays(10)));
    }

    @Test
    @DisplayName("참가비 단계 없이 얼리버드 마감만 아는 대회는 \"얼리버드\"")
    void earlyBird_withoutTiers() {
        assertThat(CompetitionDeadline.dueOn(competition(D, D.plusDays(5)), D))
                .containsExactly(new CompetitionDeadline(CompetitionDeadlineType.EARLY_BIRD, "얼리버드", D));
    }

    @Test
    @DisplayName("그날 끝나는 마감이 없거나 날짜를 모르면 빈 목록")
    void none() {
        assertThat(CompetitionDeadline.dueOn(competition(D.plusDays(1), D.plusDays(5), fee("기본", null)), D)).isEmpty();
        assertThat(CompetitionDeadline.dueOn(competition(null, null), D)).isEmpty();
    }

    @Test
    @DisplayName("문구: 제목은 마감 이름 + 내일, 본문은 대회명과 공지 안내")
    void texts() {
        CompetitionDeadline tier = new CompetitionDeadline(CompetitionDeadlineType.EARLY_BIRD, "1차 얼리버드", D);
        CompetitionDeadline registration = new CompetitionDeadline(CompetitionDeadlineType.REGISTRATION, "접수", D);

        assertThat(tier.title()).isEqualTo("1차 얼리버드 마감 내일");
        assertThat(tier.body("서울 오픈")).isEqualTo("서울 오픈 1차 얼리버드 마감이 내일이에요. 신청과 결제는 대회 공지에서");
        assertThat(registration.title()).isEqualTo("접수 마감 내일");
        assertThat(registration.body("서울 오픈")).isEqualTo("서울 오픈 접수가 내일 끝나요. 신청과 결제는 대회 공지에서");
    }
}
