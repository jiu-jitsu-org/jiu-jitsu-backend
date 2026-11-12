package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionFee;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionUniform;
import com.jiujitsu.api.domain.competition.service.CompetitionService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CompetitionRepositoryTest {

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private EntityManager em;

    private void save(String name, Integer year) {
        competitionRepository.save(Competition.of(name, year));
    }

    // 서비스(CompetitionService.search)와 같은 규칙으로 정규화·이스케이프한다
    private List<String> search(String keyword) {
        return searchAll(keyword, 20).stream()
                .map(Competition::getName)
                .toList();
    }

    // 자동완성 기준일 (서비스는 Clock 으로 구한 오늘을 넘긴다)
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    private List<Competition> searchAll(String keyword, int limit) {
        String normalized = Competition.normalize(keyword);
        String escaped = CompetitionService.escapeLike(normalized);
        return competitionRepository.search(normalized, escaped + "%", "%" + escaped + "%", TODAY.getYear(), TODAY, PageRequest.of(0, limit));
    }

    // 개최일이 있는 공개 대회
    private Competition publishedEvent(String name, LocalDate openDate) {
        return Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(openDate.getYear())
                .published(true)
                .openDate(openDate)
                .build();
    }

    @Test
    @DisplayName("대회명 일부로 등록된 대회를 찾는다")
    void searchPartial() {
        save("2026 서울 오픈 주짓수 챔피언십", 2026);
        save("부산 오픈", 2026);

        assertThat(search("서울")).containsExactly("2026 서울 오픈 주짓수 챔피언십");
    }

    @Test
    @DisplayName("공백·대소문자를 무시하고 찾는다")
    void searchIgnoresSpaceAndCase() {
        save("서울 오픈", 2026);
        save("Seoul Open", 2025);

        assertThat(search("서울오픈")).containsExactly("서울 오픈");
        assertThat(search("SEOUL open")).containsExactly("Seoul Open");
    }

    @Test
    @DisplayName("연도 최신순, 연도 없는 대회는 뒤, 같은 연도는 이름순으로 정렬하고 개수를 제한한다")
    void searchOrderAndLimit() {
        save("오픈 C 연도없음", null);
        save("오픈 B", 2024);
        save("오픈 A2", 2026);
        save("오픈 A1", 2026);

        assertThat(search("오픈")).containsExactly("오픈 A1", "오픈 A2", "오픈 B", "오픈 C 연도없음");

        IntStream.range(0, 25).forEach(i -> save("리그 " + i, 2026));
        assertThat(searchAll("리그", 20)).hasSize(20);
    }

    @Test
    @DisplayName("이스케이프한 % _ 는 문자 그대로 찾는다")
    void searchEscapesWildcard() {
        save("100%오픈", 2026);
        save("100점오픈", 2026);
        save("a_b컵", 2026);
        save("axb컵", 2026);

        assertThat(search("100%")).containsExactly("100%오픈");
        assertThat(search("a_b")).containsExactly("a_b컵");
    }

    @Test
    @DisplayName("정확 일치 → 앞부분 일치 → 부분 일치 순으로 먼저 보여 준다 (연도보다 우선)")
    void searchRanksExactAndPrefixFirst() {
        IntStream.range(0, 25).forEach(i -> save("코리아 오픈 " + i, 2026));
        save("오픈 리그", 2020);
        save("오픈", null);

        List<String> result = search("오픈");

        assertThat(result).hasSize(20);
        assertThat(result.subList(0, 2)).containsExactly("오픈", "오픈 리그");
    }

    @Test
    @DisplayName("같은 이름이라도 연도가 다르면 따로 저장되고, 정규화 이름과 연도가 모두 같으면 저장할 수 없다")
    void uniqueSearchNameAndYear() {
        save("서울 오픈", 2026);
        competitionRepository.saveAndFlush(Competition.of("서울 오픈", 2025));

        assertThat(search("서울오픈")).containsExactly("서울 오픈", "서울 오픈");
        assertThat(searchAll("서울오픈", 20)).extracting(Competition::getCompetitionYear).containsExactly(2026, 2025);
        assertThatThrownBy(() -> competitionRepository.saveAndFlush(Competition.of("서울오픈", 2026)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("maxYear 이후 예정 대회는 빼고, 연도 모르는 대회는 남긴다")
    void searchExcludesFutureCompetitions() {
        save("서울 오픈", 2027);
        save("서울 오픈", 2026);
        save("서울 컵", null);

        assertThat(searchAll("서울", 20)).extracting(Competition::getName, Competition::getCompetitionYear)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("서울 오픈", 2026),
                        org.assertj.core.groups.Tuple.tuple("서울 컵", null));
    }

    @Test
    @DisplayName("개최일이 있는 대회는 올해라도 오늘 이후면 빼고, 오늘(당일)까지 열린 대회는 남긴다")
    void searchExcludesNotYetHeldEventsThisYear() {
        competitionRepository.save(publishedEvent("부산 오픈", TODAY.minusDays(1)));
        competitionRepository.save(publishedEvent("대구 오픈", TODAY));
        competitionRepository.save(publishedEvent("광주 오픈", TODAY.plusDays(14)));

        assertThat(search("오픈")).containsExactlyInAnyOrder("부산 오픈", "대구 오픈");
    }

    @Test
    @DisplayName("종목은 중복 없이 저장되고 다시 불러올 수 있다")
    void savesUniforms() {
        Competition event = Competition.builder()
                .name("서울 오픈").searchName(Competition.normalize("서울 오픈")).competitionYear(2026)
                .published(true).openDate(LocalDate.of(2026, 11, 22))
                .uniforms(new LinkedHashSet<>(List.of(CompetitionUniform.GI, CompetitionUniform.NO_GI)))
                .build();
        Long id = competitionRepository.saveAndFlush(event).getId();
        em.clear();

        assertThat(competitionRepository.findById(id).orElseThrow().getUniforms())
                .containsExactlyInAnyOrder(CompetitionUniform.GI, CompetitionUniform.NO_GI);
    }

    @Test
    @DisplayName("참가비 단계·시작 시각·경기장이 입력 순서대로 저장되고, 대회를 지우면 단계도 함께 지워진다")
    void savesFeesInOrder_andCascades() {
        LocalDate earlyBird = LocalDate.of(2026, 11, 8);
        Competition event = Competition.builder()
                .name("서울 오픈").searchName(Competition.normalize("서울 오픈")).competitionYear(2026)
                .published(true).openDate(LocalDate.of(2026, 11, 22)).startTime(LocalTime.of(9, 0))
                .place("서울 송파").venue("올림픽홀")
                .earlyBirdDeadline(earlyBird).registrationDeadline(LocalDate.of(2026, 11, 15))
                .fees(new ArrayList<>(List.of(
                        new CompetitionFee("1차 얼리버드", 50000, LocalDate.of(2026, 11, 1)),
                        new CompetitionFee("2차 얼리버드", 60000, earlyBird),
                        new CompetitionFee("기본 참가비", 70000, null))))
                .build();
        Long id = competitionRepository.saveAndFlush(event).getId();
        em.clear();

        Competition saved = competitionRepository.findById(id).orElseThrow();
        assertThat(saved.getFees()).extracting(CompetitionFee::getLabel).containsExactly("1차 얼리버드", "2차 얼리버드", "기본 참가비");
        assertThat(saved.getFees().get(2).getUntil()).isNull();
        assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(saved.getVenue()).isEqualTo("올림픽홀");

        em.createNativeQuery("DELETE FROM competitions WHERE id = " + id).executeUpdate();
        Number left = (Number) em.createNativeQuery("SELECT COUNT(*) FROM competition_fees WHERE competition_id = " + id).getSingleResult();
        assertThat(left.longValue()).isZero();
    }

    @Test
    @DisplayName("incrementViewCount: DB 에서 +1, 미공개 카탈로그 엔트리는 바뀌지 않고 0행")
    void incrementViewCount() {
        Long published = competitionRepository.saveAndFlush(publishedEvent("서울 오픈", TODAY.plusDays(7))).getId();
        Long catalog = competitionRepository.saveAndFlush(Competition.of("부산 오픈", 2025)).getId();

        assertThat(competitionRepository.incrementViewCount(published)).isEqualTo(1);
        assertThat(competitionRepository.incrementViewCount(published)).isEqualTo(1);
        assertThat(competitionRepository.incrementViewCount(catalog)).isZero();
        assertThat(competitionRepository.incrementViewCount(-1L)).isZero();

        assertThat(competitionRepository.findViewCountById(published)).isEqualTo(2);
        assertThat(competitionRepository.findViewCountById(catalog)).isZero();
    }

    @Test
    @DisplayName("불변식을 어긴 대회는 저장 직전에 COMPETITION_INVALID_EVENT_INFO 로 막는다")
    void rejectsInvalidEventInfoOnPersist() {
        Competition noOpenDate = Competition.builder()
                .name("서울 오픈").searchName(Competition.normalize("서울 오픈")).competitionYear(2026)
                .published(true)
                .build();

        assertThatThrownBy(() -> competitionRepository.saveAndFlush(noOpenDate))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
    }

    @Test
    @DisplayName("기존 카탈로그 엔트리(이름+연도)는 미공개·조회수 0 으로 저장된다")
    void catalogEntryDefaults() {
        Long id = competitionRepository.saveAndFlush(Competition.of("서울 오픈", 2026)).getId();
        em.clear();

        Competition saved = competitionRepository.findById(id).orElseThrow();
        assertThat(saved.isPublished()).isFalse();
        assertThat(saved.getViewCount()).isZero();
        assertThat(saved.getUniforms()).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndPublishedTrue: 공개 대회만 찾고 미공개 카탈로그 엔트리는 빈 결과")
    void findByIdAndPublishedTrue() {
        Competition published = competitionRepository.save(publishedEvent("서울 오픈", TODAY.plusDays(7)));
        Competition catalog = competitionRepository.save(Competition.of("부산 오픈", 2025));

        assertThat(competitionRepository.findByIdAndPublishedTrue(published.getId())).contains(published);
        assertThat(competitionRepository.findByIdAndPublishedTrue(catalog.getId())).isEmpty();
    }

    // 지역이 있는 공개 대회
    private void savePublishedIn(String name, CompetitionRegion region, LocalDate openDate) {
        competitionRepository.save(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(openDate.getYear())
                .published(true)
                .region(region)
                .openDate(openDate)
                .build());
    }

    @Test
    @DisplayName("findPublishedRegions: 공개 대회가 있는 지역을 중복 없이")
    void findPublishedRegionsDistinct() {
        savePublishedIn("서울 오픈", CompetitionRegion.SEOUL, TODAY.plusDays(7));
        savePublishedIn("서울 챔피언십", CompetitionRegion.SEOUL, TODAY.plusDays(14));
        savePublishedIn("부산 오픈", CompetitionRegion.BUSAN, TODAY.plusDays(21));

        assertThat(competitionRepository.findPublishedRegions())
                .containsExactlyInAnyOrder(CompetitionRegion.SEOUL, CompetitionRegion.BUSAN);
    }

    @Test
    @DisplayName("findPublishedRegions: 지난 대회만 있는 지역도 포함한다")
    void findPublishedRegionsIncludesPast() {
        savePublishedIn("울산 오픈", CompetitionRegion.ULSAN, TODAY.minusDays(30));

        assertThat(competitionRepository.findPublishedRegions()).containsExactly(CompetitionRegion.ULSAN);
    }

    @Test
    @DisplayName("findPublishedRegions: 미공개 카탈로그 엔트리의 지역은 뺀다")
    void findPublishedRegionsExcludesUnpublished() {
        competitionRepository.save(Competition.builder()
                .name("제주 오픈").searchName(Competition.normalize("제주 오픈")).competitionYear(2026)
                .region(CompetitionRegion.JEJU)
                .build());

        assertThat(competitionRepository.findPublishedRegions()).isEmpty();
    }

    @Test
    @DisplayName("findPublishedRegions: 지역이 없는 공개 대회는 어느 지역에도 넣지 않는다")
    void findPublishedRegionsExcludesNullRegion() {
        competitionRepository.save(publishedEvent("지역 미정 오픈", TODAY.plusDays(7)));
        savePublishedIn("대전 오픈", CompetitionRegion.DAEJEON, TODAY.plusDays(7));

        assertThat(competitionRepository.findPublishedRegions()).containsExactly(CompetitionRegion.DAEJEON);
    }
}
