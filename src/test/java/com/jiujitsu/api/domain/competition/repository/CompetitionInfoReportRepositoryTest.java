package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoField;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CompetitionInfoReportRepositoryTest {

    private static final LocalDateTime START_OF_TODAY = LocalDate.of(2026, 11, 11).atStartOfDay();

    @Autowired
    private CompetitionInfoReportRepository competitionInfoReportRepository;

    @Autowired
    private TestEntityManager em;

    private User saveUser(String nickname) {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Competition saveCompetition(String name) {
        return em.persist(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(2026)
                .published(true)
                .openDate(LocalDate.of(2026, 12, 6))
                .build());
    }

    // 제보 시각을 정해 저장 — 감사(@CreatedDate) 값과 상관없이 SQL 로 덮어쓴다
    private CompetitionInfoReport report(Competition competition, User user, CompetitionInfoField field, LocalDateTime createdAt) {
        CompetitionInfoReport report = em.persistAndFlush(CompetitionInfoReport.of(competition, user, field, "내용"));
        em.getEntityManager().createNativeQuery("UPDATE competition_info_reports SET created_at = :at WHERE id = :id")
                .setParameter("at", createdAt)
                .setParameter("id", report.getId())
                .executeUpdate();
        return report;
    }

    @Test
    @DisplayName("하루 횟수: 오늘 자정 이후, 같은 대회·유저·항목만 센다 (어제·다른 항목·다른 유저·다른 대회 제외)")
    void countSince() {
        User me = saveUser("나");
        User other = saveUser("다른 사람");
        Competition seoul = saveCompetition("서울 오픈");
        Competition busan = saveCompetition("부산 오픈");

        report(seoul, me, CompetitionInfoField.FEE, START_OF_TODAY);                 // 자정 정각 — 셈
        report(seoul, me, CompetitionInfoField.FEE, START_OF_TODAY.plusHours(14));   // 셈
        report(seoul, me, CompetitionInfoField.FEE, START_OF_TODAY.minusSeconds(1)); // 어제
        report(seoul, me, CompetitionInfoField.PLACE, START_OF_TODAY.plusHours(1));  // 다른 항목
        report(seoul, other, CompetitionInfoField.FEE, START_OF_TODAY.plusHours(1)); // 다른 유저
        report(busan, me, CompetitionInfoField.FEE, START_OF_TODAY.plusHours(1));    // 다른 대회
        em.clear();

        assertThat(competitionInfoReportRepository.countSince(seoul.getId(), me.getId(), CompetitionInfoField.FEE, START_OF_TODAY))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("관리자 목록: 최신순(같은 시각은 id 역순)으로, 대회·제보자를 함께 가져온다")
    void findAllWithCompetitionAndUser() {
        User me = saveUser("나");
        Competition seoul = saveCompetition("서울 오픈");
        CompetitionInfoReport oldest = report(seoul, me, CompetitionInfoField.FEE, START_OF_TODAY.minusDays(1));
        CompetitionInfoReport sameTimeFirst = report(seoul, me, CompetitionInfoField.PLACE, START_OF_TODAY);
        CompetitionInfoReport sameTimeSecond = report(seoul, me, CompetitionInfoField.SCHEDULE, START_OF_TODAY);
        em.clear();

        Page<CompetitionInfoReport> first = competitionInfoReportRepository.findAllWithCompetitionAndUser(PageRequest.of(0, 2));
        Page<CompetitionInfoReport> second = competitionInfoReportRepository.findAllWithCompetitionAndUser(PageRequest.of(1, 2));

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).extracting(CompetitionInfoReport::getId)
                .containsExactly(sameTimeSecond.getId(), sameTimeFirst.getId());
        assertThat(second.getContent()).extracting(CompetitionInfoReport::getId).containsExactly(oldest.getId());
        CompetitionInfoReport loaded = first.getContent().get(0);
        assertThat(loaded.getCompetition().getName()).isEqualTo("서울 오픈");
        assertThat(loaded.getUser().getNickname()).isEqualTo("나");
    }

    @Test
    @DisplayName("대회가 지워지면 그 대회의 제보도 DB에서 함께 지워진다 (ON DELETE CASCADE)")
    void deleteCompetition_cascades() {
        User user = saveUser("나");
        Competition seoul = saveCompetition("서울 오픈");
        report(seoul, user, CompetitionInfoField.FEE, START_OF_TODAY);
        em.clear();

        // 엔티티 삭제가 아니라 DB FK 동작을 보기 위해 SQL 로 지운다
        em.getEntityManager().createNativeQuery("DELETE FROM competitions WHERE id = " + seoul.getId()).executeUpdate();

        assertThat(competitionInfoReportRepository.count()).isZero();
    }

    @Test
    @DisplayName("유저가 지워지면 그 유저의 제보도 DB에서 함께 지워진다 (ON DELETE CASCADE)")
    void deleteUser_cascades() {
        User user = saveUser("나");
        Competition seoul = saveCompetition("서울 오픈");
        report(seoul, user, CompetitionInfoField.FEE, START_OF_TODAY);
        em.clear();

        em.getEntityManager().createNativeQuery("DELETE FROM users WHERE id = " + user.getId()).executeUpdate();

        assertThat(competitionInfoReportRepository.count()).isZero();
    }
}
