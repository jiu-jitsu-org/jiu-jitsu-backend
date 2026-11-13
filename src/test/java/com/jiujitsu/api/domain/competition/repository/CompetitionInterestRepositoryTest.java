package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CompetitionInterestRepositoryTest {

    @Autowired
    private CompetitionInterestRepository competitionInterestRepository;

    @Autowired
    private TestEntityManager em;

    private User saveUser() {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
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

    private void interest(Competition competition, User user) {
        em.persist(new CompetitionInterest(competition, user));
    }

    private Map<Long, Long> counts(List<Long> ids) {
        return competitionInterestRepository.countByCompetitionIds(ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }

    @Test
    @DisplayName("같은 유저가 같은 대회에 두 번 관심 등록하면 유니크 제약에 걸린다")
    void duplicate_violatesUnique() {
        User user = saveUser();
        Competition competition = saveCompetition("서울 오픈");
        interest(competition, user);
        em.flush();

        assertThatThrownBy(() -> competitionInterestRepository.saveAndFlush(new CompetitionInterest(competition, user)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("대회별 관심 수를 한 번에 집계하고, 관심이 없는 대회는 결과에 없다")
    void countByCompetitionIds() {
        User a = saveUser();
        User b = saveUser();
        Competition seoul = saveCompetition("서울 오픈");
        Competition busan = saveCompetition("부산 오픈");
        Competition daegu = saveCompetition("대구 오픈");
        interest(seoul, a);
        interest(seoul, b);
        interest(busan, a);
        em.flush();

        assertThat(counts(List.of(seoul.getId(), busan.getId(), daegu.getId())))
                .containsOnly(Map.entry(seoul.getId(), 2L), Map.entry(busan.getId(), 1L));
        assertThat(competitionInterestRepository.countByCompetitionId(daegu.getId())).isZero();
    }

    @Test
    @DisplayName("주어진 대회 중 내가 관심 등록한 대회 id만 돌려준다")
    void findInterestedCompetitionIds() {
        User me = saveUser();
        User other = saveUser();
        Competition seoul = saveCompetition("서울 오픈");
        Competition busan = saveCompetition("부산 오픈");
        Competition daegu = saveCompetition("대구 오픈");
        interest(seoul, me);
        interest(busan, other);
        interest(daegu, me);
        em.flush();

        assertThat(competitionInterestRepository.findInterestedCompetitionIds(me.getId(), List.of(seoul.getId(), busan.getId())))
                .containsExactly(seoul.getId());
    }

    @Test
    @DisplayName("관심 해제는 내 관심만 지운다")
    void deleteByCompetitionIdAndUserId() {
        User me = saveUser();
        User other = saveUser();
        Competition seoul = saveCompetition("서울 오픈");
        interest(seoul, me);
        interest(seoul, other);
        em.flush();

        competitionInterestRepository.deleteByCompetitionIdAndUserId(seoul.getId(), me.getId());

        assertThat(competitionInterestRepository.existsByCompetitionIdAndUserId(seoul.getId(), me.getId())).isFalse();
        assertThat(competitionInterestRepository.existsByCompetitionIdAndUserId(seoul.getId(), other.getId())).isTrue();
    }

    @Test
    @DisplayName("대회가 지워지면 그 대회의 관심도 DB에서 함께 지워진다 (ON DELETE CASCADE)")
    void deleteCompetition_cascades() {
        User user = saveUser();
        Competition seoul = saveCompetition("서울 오픈");
        interest(seoul, user);
        em.flush();
        em.clear();

        // 엔티티 삭제가 아니라 DB FK 동작을 보기 위해 SQL 로 지운다
        em.getEntityManager().createNativeQuery("DELETE FROM competitions WHERE id = " + seoul.getId()).executeUpdate();

        assertThat(competitionInterestRepository.count()).isZero();
    }

    @Test
    @DisplayName("유저가 지워지면 그 유저의 관심도 DB에서 함께 지워진다 (ON DELETE CASCADE)")
    void deleteUser_cascades() {
        User user = saveUser();
        Competition seoul = saveCompetition("서울 오픈");
        interest(seoul, user);
        em.flush();
        em.clear();

        em.getEntityManager().createNativeQuery("DELETE FROM users WHERE id = " + user.getId()).executeUpdate();

        assertThat(competitionInterestRepository.count()).isZero();
    }

    // ---- 관심 많은 대회 (#210) ----

    private static final LocalDate TODAY = LocalDate.of(2026, 12, 10);

    private Competition saveOn(String name, LocalDate openDate, boolean published) {
        return em.persist(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(openDate.getYear())
                .published(published)
                .openDate(openDate)
                .build());
    }

    private Competition withInterests(Competition competition, int count) {
        for (int i = 0; i < count; i++) {
            interest(competition, saveUser());
        }
        return competition;
    }

    private List<Long> popular(int top, long min) {
        return competitionInterestRepository.findPopularIds(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), TODAY,
                min, PageRequest.of(0, top));
    }

    @Test
    @DisplayName("관심 많은 대회: 같은 달 관심 수 상위 N개, 관심 M개 미만은 빠지고, 동점은 대회일 빠른 순")
    void findPopularIds_topNAboveMin() {
        Competition seven = withInterests(saveOn("일곱", LocalDate.of(2026, 12, 20), true), 7);
        Competition six = withInterests(saveOn("여섯", LocalDate.of(2026, 12, 27), true), 6);
        Competition fiveEarly = withInterests(saveOn("다섯 앞", LocalDate.of(2026, 12, 12), true), 5);
        withInterests(saveOn("다섯 뒤", LocalDate.of(2026, 12, 13), true), 5);
        withInterests(saveOn("넷", LocalDate.of(2026, 12, 14), true), 4);

        assertThat(popular(3, 5)).containsExactly(seven.getId(), six.getId(), fiveEarly.getId());
        assertThat(popular(10, 5)).hasSize(4);
    }

    @Test
    @DisplayName("관심 많은 대회: 끝난 대회(대회일 < 오늘)·다른 달·미공개 대회는 순위에 없다, 오늘 대회는 들어간다")
    void findPopularIds_excludesEndedOtherMonthUnpublished() {
        withInterests(saveOn("끝남", LocalDate.of(2026, 12, 6), true), 9);
        withInterests(saveOn("다음 달", LocalDate.of(2027, 1, 3), true), 9);
        withInterests(saveOn("미공개", LocalDate.of(2026, 12, 20), false), 9);
        Competition today = withInterests(saveOn("오늘", LocalDate.of(2026, 12, 10), true), 5);

        assertThat(popular(3, 5)).containsExactly(today.getId());
    }
}

