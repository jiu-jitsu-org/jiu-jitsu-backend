package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionStatus;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.jiujitsu.api.domain.competition.repository.CompetitionSpecifications.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CompetitionSpecificationsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);
    private static final Sort ORDER = Sort.by(Sort.Order.asc("openDate"), Sort.Order.asc("id"));

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private TestEntityManager em;

    private Competition save(String name, LocalDate openDate, LocalDate start, LocalDate deadline) {
        return save(name, openDate, start, deadline, CompetitionRegion.SEOUL);
    }

    private Competition save(String name, LocalDate openDate, LocalDate start, LocalDate deadline, CompetitionRegion region) {
        return save(name, openDate, null, start, deadline, region);
    }

    private Competition save(String name, LocalDate openDate, LocalDate endDate, LocalDate start, LocalDate deadline, CompetitionRegion region) {
        return em.persist(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(openDate.getYear())
                .published(true)
                .openDate(openDate)
                .endDate(endDate)
                .registrationStartDate(start)
                .registrationDeadline(deadline)
                .region(region)
                .build());
    }

    private User saveUser() {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private List<String> names(Specification<Competition> spec) {
        return competitionRepository.findAll(spec, ORDER).stream().map(Competition::getName).toList();
    }

    private LocalDate day(int offset) {
        return TODAY.plusDays(offset);
    }

    @Test
    @DisplayName("status 조건은 엔티티 status(today) 판단과 같다 — 일정 경계(당일·하루 전후·비어 있는 값)·종료일(하루짜리·진행 중·오늘 끝남·어제 끝남) 조합 전부")
    void statusIn_matchesEntityStatus() {
        // 대회일: 그제·어제·오늘·미래, 종료일: 없음(하루짜리)·대회일+1·+2, 접수 시작: 없음·내일·오늘·어제, 접수 마감: 없음·어제·오늘·내일
        List<LocalDate> openDates = List.of(day(-2), day(-1), day(0), day(30));
        List<Integer> extraDays = java.util.Arrays.asList(null, 1, 2);
        List<LocalDate> starts = java.util.Arrays.asList(null, day(1), day(0), day(-10));
        List<LocalDate> deadlines = java.util.Arrays.asList(null, day(-1), day(0), day(1));
        int n = 0;
        for (LocalDate openDate : openDates) {
            for (Integer extra : extraDays) {
                LocalDate endDate = extra == null ? null : openDate.plusDays(extra);
                for (LocalDate start : starts) {
                    for (LocalDate deadline : deadlines) {
                        // 일정 순서(접수 시작 ≤ 마감 ≤ 대회일)를 어기는 조합은 저장할 수 없으니 건너뛴다
                        if ((start != null && deadline != null && start.isAfter(deadline))
                                || (start != null && start.isAfter(openDate))
                                || (deadline != null && deadline.isAfter(openDate))) {
                            continue;
                        }
                        save("대회" + n++, openDate, endDate, start, deadline, CompetitionRegion.SEOUL);
                    }
                }
            }
        }
        em.flush();
        List<Competition> all = competitionRepository.findAll();

        for (CompetitionStatus status : CompetitionStatus.values()) {
            List<Long> expected = all.stream().filter(c -> c.status(TODAY) == status).map(Competition::getId).sorted().toList();
            List<Long> actual = competitionRepository.findAll(statusIn(Set.of(status), TODAY)).stream().map(Competition::getId).sorted().toList();
            assertThat(actual).as(status.name()).isEqualTo(expected);
        }
        // 여러 상태는 OR
        List<Long> openOrUpcoming = all.stream().filter(c -> EnumSet.of(CompetitionStatus.OPEN, CompetitionStatus.UPCOMING).contains(c.status(TODAY)))
                .map(Competition::getId).sorted().toList();
        assertThat(competitionRepository.findAll(statusIn(Set.of(CompetitionStatus.OPEN, CompetitionStatus.UPCOMING), TODAY))
                .stream().map(Competition::getId).sorted().toList()).isEqualTo(openOrUpcoming);
        // 네 상태가 공개 대회를 빠짐없이 나눈다
        assertThat(CompetitionStatus.values()).allSatisfy(s -> assertThat(competitionRepository.findAll(statusIn(Set.of(s), TODAY))).isNotEmpty());
        assertThat(competitionRepository.findAll(statusIn(EnumSet.allOf(CompetitionStatus.class), TODAY))).hasSameSizeAs(all);
        // 끝남 조건(고르기용)도 엔티티 isEnded 와 같은 조합 전부에서 같다
        assertThat(competitionRepository.findAll(ended(TODAY)).stream().map(Competition::getId).sorted().toList())
                .isEqualTo(all.stream().filter(c -> c.isEnded(TODAY)).map(Competition::getId).sorted().toList());
        assertThat(competitionRepository.findAll(notEnded(TODAY)).stream().map(Competition::getId).sorted().toList())
                .isEqualTo(all.stream().filter(c -> !c.isEnded(TODAY)).map(Competition::getId).sorted().toList());
    }

    @Test
    @DisplayName("notEnded·ended: 마지막 날(종료일, 하루짜리면 대회일) 기준 — 진행 중인 여러 날 대회·오늘 끝나는 대회는 끝나지 않음, 어제 끝난 대회는 끝남 (#223)")
    void notEndedAndEnded_byLastDay() {
        save("어제 하루", day(-1), null, null, null, CompetitionRegion.SEOUL);
        save("어제 끝난 이틀", day(-2), day(-1), null, null, CompetitionRegion.SEOUL);
        save("오늘 끝나는 이틀", day(-1), day(0), null, null, CompetitionRegion.SEOUL);
        save("진행 중 사흘", day(-1), day(1), null, null, CompetitionRegion.SEOUL);
        save("오늘 하루", day(0), null, null, null, CompetitionRegion.SEOUL);
        save("미래", day(7), null, null, null, CompetitionRegion.SEOUL);
        em.flush();

        assertThat(names(notEnded(TODAY))).containsExactly("오늘 끝나는 이틀", "진행 중 사흘", "오늘 하루", "미래");
        assertThat(names(ended(TODAY))).containsExactly("어제 끝난 이틀", "어제 하루");
    }

    @Test
    @DisplayName("published: 미공개 카탈로그 엔트리는 빠진다")
    void published_excludesCatalog() {
        save("서울 오픈", day(10), null, null);
        em.persist(Competition.of("부산 오픈", 2026));

        assertThat(names(published())).containsExactly("서울 오픈");
    }

    @Test
    @DisplayName("after·before: (openDate, id) 경계 기준, 같은 날은 id 로 가르고 경계 자신은 빠진다")
    void afterBefore_keyset() {
        Competition a = save("A", day(1), null, null);
        Competition b = save("B", day(2), null, null);
        Competition c = save("C", day(2), null, null);
        save("D", day(3), null, null);

        assertThat(names(after(b.getOpenDate(), b.getId()))).containsExactly("C", "D");
        assertThat(names(before(c.getOpenDate(), c.getId()))).containsExactly("A", "B");
        assertThat(names(after(day(2), 0))).containsExactly("B", "C", "D");   // 기준일 포함
        assertThat(names(before(day(2), 1))).containsExactly("A");            // 기준일 전부 제외
        assertThat(names(after(a.getOpenDate(), a.getId()).and(before(day(3), 0)))).containsExactly("B", "C");
    }

    @Test
    @DisplayName("regionIn: 고른 시·도 중 하나, 지역 없는 대회는 빠진다")
    void regionIn_any() {
        save("서울", day(1), null, null, CompetitionRegion.SEOUL);
        save("경기", day(1), null, null, CompetitionRegion.GYEONGGI);
        save("부산", day(1), null, null, CompetitionRegion.BUSAN);
        save("미정", day(1), null, null, null);

        assertThat(names(regionIn(List.of(CompetitionRegion.SEOUL, CompetitionRegion.GYEONGGI)))).containsExactlyInAnyOrder("서울", "경기");
    }

    @Test
    @DisplayName("searchNameLike: 정규화 이름 부분 일치, LIKE 특수문자는 이스케이프된 그대로")
    void searchNameLike_contains() {
        save("2026 서울 오픈", day(1), null, null);
        save("부산 오픈", day(1), null, null);
        save("100% 챌린지", day(1), null, null);

        assertThat(names(searchNameLike("%오픈%"))).containsExactlyInAnyOrder("2026 서울 오픈", "부산 오픈");
        assertThat(names(searchNameLike("%100\\%%"))).containsExactly("100% 챌린지");
        assertThat(names(searchNameLike("%0\\%%"))).containsExactly("100% 챌린지");
    }

    @Test
    @DisplayName("interestedBy: 이 유저가 관심 등록한 대회만, 다른 유저 관심 수만큼 늘지 않는다")
    void interestedBy_onlyMine() {
        User me = saveUser();
        User other = saveUser();
        Competition seoul = save("서울", day(1), null, null);
        Competition busan = save("부산", day(2), null, null);
        save("대구", day(3), null, null);
        em.persist(new CompetitionInterest(seoul, me));
        em.persist(new CompetitionInterest(seoul, other));
        em.persist(new CompetitionInterest(busan, other));

        assertThat(names(interestedBy(me.getId()))).containsExactly("서울");
    }

    @Test
    @DisplayName("조건은 AND 로 겹친다")
    void combined_and() {
        save("서울 오픈", day(1), null, null, CompetitionRegion.SEOUL);
        save("서울 컵", day(-1), null, null, CompetitionRegion.SEOUL);
        save("부산 오픈", day(1), null, null, CompetitionRegion.BUSAN);

        Specification<Competition> spec = published()
                .and(regionIn(List.of(CompetitionRegion.SEOUL)))
                .and(statusIn(Set.of(CompetitionStatus.OPEN), TODAY))
                .and(searchNameLike("%서울%"));
        assertThat(names(spec)).containsExactly("서울 오픈");
    }
}
