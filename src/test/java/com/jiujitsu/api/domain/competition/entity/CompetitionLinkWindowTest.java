package com.jiujitsu.api.domain.competition.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CompetitionLinkWindowTest {

    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 11, 10, 14, 0);

    // 대회 12/5~12/6, 접수 11/1~11/30
    private static Competition.CompetitionBuilder twoDay() {
        return Competition.builder()
                .name("서울 오픈").searchName("서울오픈").competitionYear(2026).published(true)
                .openDate(LocalDate.of(2026, 12, 5)).endDate(LocalDate.of(2026, 12, 6))
                .registrationStartDate(LocalDate.of(2026, 11, 1)).registrationDeadline(LocalDate.of(2026, 11, 30));
    }

    private static CompetitionLink approved(Competition competition, CompetitionLinkType type) {
        CompetitionLink link = new CompetitionLink(competition, type, "https://example.com/" + type);
        link.approve(APPROVED_AT);
        return link;
    }

    private static CompetitionLinkWindow window(Competition competition, CompetitionLinkType type) {
        return CompetitionLinkWindow.of(approved(competition, type), competition);
    }

    @Test
    @DisplayName("신청: 접수 시작일 00:00 ~ 접수 마감일 23:59:59")
    void registration() {
        assertThat(window(twoDay().build(), CompetitionLinkType.REGISTRATION)).isEqualTo(new CompetitionLinkWindow(
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 30, 23, 59, 59)));
    }

    @Test
    @DisplayName("신청: 접수 시작이 없으면 승인 시각부터, 접수 마감이 없으면 대회 전날 23:59:59까지")
    void registration_missingDates() {
        Competition competition = twoDay().registrationStartDate(null).registrationDeadline(null).build();

        assertThat(window(competition, CompetitionLinkType.REGISTRATION)).isEqualTo(new CompetitionLinkWindow(
                APPROVED_AT, LocalDateTime.of(2026, 12, 4, 23, 59, 59)));
    }

    @Test
    @DisplayName("참가자 현황 · 대진표: 승인 시각 ~ 마지막 날(종료일) 23:59:59, 승인 전이면 시작 없음")
    void entryListAndBracket() {
        Competition competition = twoDay().build();
        CompetitionLinkWindow expected = new CompetitionLinkWindow(APPROVED_AT, LocalDateTime.of(2026, 12, 6, 23, 59, 59));

        assertThat(window(competition, CompetitionLinkType.ENTRY_LIST)).isEqualTo(expected);
        assertThat(window(competition, CompetitionLinkType.BRACKET)).isEqualTo(expected);
        CompetitionLink pending = new CompetitionLink(competition, CompetitionLinkType.BRACKET, "https://example.com");
        assertThat(CompetitionLinkWindow.of(pending, competition).from()).isNull();
    }

    @Test
    @DisplayName("실시간 매트: 대회 첫날 00:00 ~ 마지막 날 23:59:59, 하루짜리면 대회일 하루")
    void liveMat() {
        assertThat(window(twoDay().build(), CompetitionLinkType.LIVE_MAT)).isEqualTo(new CompetitionLinkWindow(
                LocalDateTime.of(2026, 12, 5, 0, 0), LocalDateTime.of(2026, 12, 6, 23, 59, 59)));
        assertThat(window(twoDay().endDate(null).build(), CompetitionLinkType.LIVE_MAT)).isEqualTo(new CompetitionLinkWindow(
                LocalDateTime.of(2026, 12, 5, 0, 0), LocalDateTime.of(2026, 12, 5, 23, 59, 59)));
    }

    @Test
    @DisplayName("결과: 마지막 날 다음 날 00:00 ~ 14일 뒤 23:59:59, 결과 주소가 고정인 사이트(FLOWCOMP·주대플·대한체육회)는 끝 없음")
    void result() {
        assertThat(window(twoDay().registrationSite(RegistrationSite.STREET_JIUJITSU).build(), CompetitionLinkType.RESULT))
                .isEqualTo(new CompetitionLinkWindow(LocalDateTime.of(2026, 12, 7, 0, 0), LocalDateTime.of(2026, 12, 20, 23, 59, 59)));
        assertThat(window(twoDay().build(), CompetitionLinkType.RESULT).until()).isNotNull();   // 모름 = 기타
        for (RegistrationSite site : new RegistrationSite[]{RegistrationSite.FLOWCOMP, RegistrationSite.JIUDAEPL, RegistrationSite.KSOC}) {
            assertThat(window(twoDay().registrationSite(site).build(), CompetitionLinkType.RESULT))
                    .isEqualTo(new CompetitionLinkWindow(LocalDateTime.of(2026, 12, 7, 0, 0), null));
        }
    }

    @Test
    @DisplayName("보임: 승인 + 기간 안(양 끝 포함)만. 승인 전 · 숨김 · 기간 밖은 안 보임")
    void isVisible() {
        Competition competition = twoDay().build();
        CompetitionLink live = approved(competition, CompetitionLinkType.LIVE_MAT);

        assertThat(CompetitionLinkWindow.isVisible(live, competition, LocalDateTime.of(2026, 12, 4, 23, 59, 59))).isFalse();
        assertThat(CompetitionLinkWindow.isVisible(live, competition, LocalDateTime.of(2026, 12, 5, 0, 0))).isTrue();
        assertThat(CompetitionLinkWindow.isVisible(live, competition, LocalDateTime.of(2026, 12, 6, 23, 59, 59))).isTrue();
        assertThat(CompetitionLinkWindow.isVisible(live, competition, LocalDateTime.of(2026, 12, 7, 0, 0))).isFalse();

        live.hide();
        assertThat(CompetitionLinkWindow.isVisible(live, competition, LocalDateTime.of(2026, 12, 5, 12, 0))).isFalse();
        CompetitionLink pending = new CompetitionLink(competition, CompetitionLinkType.LIVE_MAT, "https://example.com");
        assertThat(CompetitionLinkWindow.isVisible(pending, competition, LocalDateTime.of(2026, 12, 5, 12, 0))).isFalse();
    }

    @Test
    @DisplayName("신청: 접수 시작만 비면 승인 시각부터 접수 마감까지")
    void registration_startMissingOnly() {
        assertThat(window(twoDay().registrationStartDate(null).build(), CompetitionLinkType.REGISTRATION)).isEqualTo(new CompetitionLinkWindow(
                APPROVED_AT, LocalDateTime.of(2026, 11, 30, 23, 59, 59)));
    }

    @Test
    @DisplayName("끝 23:59:59 의 그 초 안(소수 초)까지 보이고, 다음 날 00:00 부터 안 보임")
    void endOfDayFractionalSecond() {
        Competition competition = twoDay().build();
        CompetitionLink live = approved(competition, CompetitionLinkType.LIVE_MAT);

        assertThat(CompetitionLinkWindow.isVisible(live, competition, LocalDateTime.of(2026, 12, 6, 23, 59, 59, 999_000_000))).isTrue();
        assertThat(CompetitionLinkWindow.isVisible(live, competition, LocalDateTime.of(2026, 12, 7, 0, 0))).isFalse();
    }

    @Test
    @DisplayName("참가자 현황을 마지막 날 뒤에 승인하면 시작이 끝보다 늦어 보이지 않음")
    void approvedAfterLastDay_neverVisible() {
        Competition competition = twoDay().build();
        CompetitionLink late = new CompetitionLink(competition, CompetitionLinkType.ENTRY_LIST, "https://example.com");
        late.approve(LocalDateTime.of(2026, 12, 8, 10, 0));

        assertThat(CompetitionLinkWindow.isVisible(late, competition, LocalDateTime.of(2026, 12, 8, 11, 0))).isFalse();
    }

    @Test
    @DisplayName("대한체육회 대회에 (동시 요청으로) 신청 링크가 남아도 보이지 않음")
    void clubRegistrationOnly_registrationNeverVisible() {
        Competition competition = twoDay().registrationSite(RegistrationSite.KSOC).build();
        CompetitionLink registration = approved(competition, CompetitionLinkType.REGISTRATION);

        assertThat(CompetitionLinkWindow.isVisible(registration, competition, LocalDateTime.of(2026, 11, 10, 12, 0))).isFalse();
    }
}

