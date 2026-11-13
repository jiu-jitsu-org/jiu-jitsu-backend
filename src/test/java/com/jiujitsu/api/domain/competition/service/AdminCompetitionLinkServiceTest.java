package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.community.profile.event.CompetitionRenamedEventListener;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionLinkResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionLinkRequest;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionLink;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkStatus;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkType;
import com.jiujitsu.api.domain.competition.entity.RegistrationSite;
import com.jiujitsu.api.domain.competition.mapper.CompetitionMapper;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 링크 넣기·승인·숨기기와 관리자 상세의 보이는 기간 — DB 와 함께 (지금 = 2026-11-13 09:00 고정)
@DataJpaTest
@Import({AdminCompetitionLinkService.class, AdminCompetitionService.class, CompetitionInterestService.class, CompetitionMapper.class,
        CompetitionRenamedEventListener.class})
class AdminCompetitionLinkServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 9, 0);

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private AdminCompetitionLinkService adminCompetitionLinkService;

    @Autowired
    private AdminCompetitionService adminCompetitionService;

    @Autowired
    private TestEntityManager em;

    @MockBean   // CompetitionInterestService 의존성
    private AuthenticationFacade authenticationFacade;

    // 대회 12/5~12/6, 접수 11/1~11/30
    private Competition save(RegistrationSite site) {
        return em.persist(Competition.builder()
                .name("서울 오픈").searchName("서울오픈").competitionYear(2026).published(true)
                .openDate(LocalDate.of(2026, 12, 5)).endDate(LocalDate.of(2026, 12, 6))
                .registrationStartDate(LocalDate.of(2026, 11, 1)).registrationDeadline(LocalDate.of(2026, 11, 30))
                .registrationSite(site)
                .build());
    }

    private static CompetitionLinkRequest url(String url) {
        return new CompetitionLinkRequest(url);
    }

    private static AdminCompetitionLinkResponse link(AdminCompetitionDetailResponse detail, CompetitionLinkType type) {
        return detail.links().stream().filter(l -> l.type() == type).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("넣으면 승인 전이라 안 보임 → 승인하면 기간 안이라 보임, 승인 시각 = 지금")
    void putThenApprove() {
        Long id = save(RegistrationSite.FLOWCOMP).getId();

        AdminCompetitionDetailResponse put = adminCompetitionLinkService.put(id, CompetitionLinkType.REGISTRATION, url(" https://flow.example/53/register "));
        assertThat(link(put, CompetitionLinkType.REGISTRATION).status()).isEqualTo(CompetitionLinkStatus.PENDING);
        assertThat(link(put, CompetitionLinkType.REGISTRATION).url()).isEqualTo("https://flow.example/53/register");
        assertThat(link(put, CompetitionLinkType.REGISTRATION).visibleNow()).isFalse();

        AdminCompetitionLinkResponse approved = link(adminCompetitionLinkService.approve(id, CompetitionLinkType.REGISTRATION), CompetitionLinkType.REGISTRATION);
        assertThat(approved.status()).isEqualTo(CompetitionLinkStatus.APPROVED);
        assertThat(approved.approvedAt()).isEqualTo(NOW);
        assertThat(approved.visibleFrom()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(approved.visibleUntil()).isEqualTo(LocalDateTime.of(2026, 11, 30, 23, 59, 59));
        assertThat(approved.visibleNow()).isTrue();
    }

    @Test
    @DisplayName("미리 승인한 결과 링크는 기간 전이라 안 보임, 숨기면 승인 상태가 꺼짐, 주소를 바꾸면 다시 승인 전")
    void resultHideAndChange() {
        Long id = save(RegistrationSite.FLOWCOMP).getId();
        adminCompetitionLinkService.put(id, CompetitionLinkType.RESULT, url("https://flow.example/53/result"));

        AdminCompetitionLinkResponse early = link(adminCompetitionLinkService.approve(id, CompetitionLinkType.RESULT), CompetitionLinkType.RESULT);
        assertThat(early.visibleNow()).isFalse();
        assertThat(early.visibleFrom()).isEqualTo(LocalDateTime.of(2026, 12, 7, 0, 0));
        assertThat(early.visibleUntil()).isNull();   // FLOWCOMP 결과는 끝 없음

        assertThat(link(adminCompetitionLinkService.hide(id, CompetitionLinkType.RESULT), CompetitionLinkType.RESULT).status())
                .isEqualTo(CompetitionLinkStatus.HIDDEN);
        AdminCompetitionLinkResponse changed = link(adminCompetitionLinkService.put(id, CompetitionLinkType.RESULT, url("https://flow.example/54/result")),
                CompetitionLinkType.RESULT);
        assertThat(changed.status()).isEqualTo(CompetitionLinkStatus.PENDING);
        assertThat(changed.approvedAt()).isNull();
    }

    @Test
    @DisplayName("종류별 하나 — 같은 종류를 다시 넣으면 덮어씀, 지우면 사라지고 없는 걸 지워도 그대로")
    void onePerTypeAndDelete() {
        Long id = save(RegistrationSite.STREET_JIUJITSU).getId();
        adminCompetitionLinkService.put(id, CompetitionLinkType.BRACKET, url("https://a.example"));
        adminCompetitionLinkService.put(id, CompetitionLinkType.BRACKET, url("https://b.example"));
        adminCompetitionLinkService.put(id, CompetitionLinkType.LIVE_MAT, url("https://c.example"));
        em.flush();

        AdminCompetitionDetailResponse detail = adminCompetitionService.getDetail(id);
        assertThat(detail.links()).extracting(AdminCompetitionLinkResponse::type)
                .containsExactly(CompetitionLinkType.BRACKET, CompetitionLinkType.LIVE_MAT);   // 종류 순서
        assertThat(link(detail, CompetitionLinkType.BRACKET).url()).isEqualTo("https://b.example");

        assertThat(adminCompetitionLinkService.delete(id, CompetitionLinkType.BRACKET).links())
                .extracting(AdminCompetitionLinkResponse::type).containsExactly(CompetitionLinkType.LIVE_MAT);
        assertThat(adminCompetitionLinkService.delete(id, CompetitionLinkType.BRACKET).links()).hasSize(1);
    }

    @Test
    @DisplayName("대한체육회 대회에 신청 링크 → R0013, 다른 종류는 됨")
    void ksocRegistrationRejected() {
        Long id = save(RegistrationSite.KSOC).getId();

        assertThatThrownBy(() -> adminCompetitionLinkService.put(id, CompetitionLinkType.REGISTRATION, url("https://ksoc.example")))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_CLUB_REGISTRATION_ONLY);
        assertThat(adminCompetitionLinkService.put(id, CompetitionLinkType.RESULT, url("https://ksoc.example/result")).links()).hasSize(1);
    }

    @Test
    @DisplayName("신청 링크가 있는 대회를 대한체육회로 고치면 R0013, 링크가 없으면 됨. 고치기에서 접수 사이트를 빼면 비움")
    void updateToKsocWithRegistrationLink() {
        Competition competition = save(RegistrationSite.FLOWCOMP);
        Long id = competition.getId();
        adminCompetitionLinkService.put(id, CompetitionLinkType.REGISTRATION, url("https://flow.example/register"));
        em.flush();

        assertThatThrownBy(() -> adminCompetitionService.update(id, request(RegistrationSite.KSOC)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_CLUB_REGISTRATION_ONLY);

        adminCompetitionLinkService.delete(id, CompetitionLinkType.REGISTRATION);
        assertThat(adminCompetitionService.update(id, request(RegistrationSite.KSOC)).registrationSite()).isEqualTo(RegistrationSite.KSOC);
        assertThat(adminCompetitionService.update(id, request(null)).registrationSite()).isNull();
    }

    @Test
    @DisplayName("없는 링크 승인·숨기기 → C0013, 자동완성용 대회 → R0011, 없는 대회 → C0010")
    void errors() {
        Long id = save(RegistrationSite.FLOWCOMP).getId();
        Long catalogId = em.persist(Competition.of("작년 대회", 2025)).getId();

        assertThatThrownBy(() -> adminCompetitionLinkService.approve(id, CompetitionLinkType.RESULT))
                .isInstanceOf(ErrorException.class).extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_LINK_NOT_FOUND);
        assertThatThrownBy(() -> adminCompetitionLinkService.hide(id, CompetitionLinkType.RESULT))
                .isInstanceOf(ErrorException.class).extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_LINK_NOT_FOUND);
        assertThatThrownBy(() -> adminCompetitionLinkService.put(catalogId, CompetitionLinkType.RESULT, url("https://x.example")))
                .isInstanceOf(ErrorException.class).extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE);
        assertThatThrownBy(() -> adminCompetitionLinkService.put(999_999L, CompetitionLinkType.RESULT, url("https://x.example")))
                .isInstanceOf(ErrorException.class).extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
    }

    @Test
    @DisplayName("대회를 지우면 링크도 같이 지워진다")
    void cascadeDelete() {
        Competition competition = save(RegistrationSite.FLOWCOMP);
        adminCompetitionLinkService.put(competition.getId(), CompetitionLinkType.RESULT, url("https://x.example"));
        em.flush();
        em.clear();

        em.getEntityManager().createNativeQuery("delete from competitions where id = " + competition.getId()).executeUpdate();

        assertThat(em.getEntityManager().createQuery("select count(l) from CompetitionLink l", Long.class).getSingleResult()).isZero();
    }

    private static AdminCompetitionRequest request(RegistrationSite site) {
        return new AdminCompetitionRequest("서울 오픈", 2026, LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 6), null, null, null, null,
                null, null, null, null, LocalDate.of(2026, 11, 1), null, LocalDate.of(2026, 11, 30), null, null, site);
    }
}
