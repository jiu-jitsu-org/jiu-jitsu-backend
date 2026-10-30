package com.jiujitsu.api.domain.community.competition.repository;

import com.jiujitsu.api.domain.community.competition.entity.Competition;
import com.jiujitsu.api.domain.community.competition.service.CompetitionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CompetitionRepositoryTest {

    @Autowired
    private CompetitionRepository competitionRepository;

    private void save(String name, Integer year) {
        competitionRepository.save(Competition.of(name, year));
    }

    // 서비스(CompetitionService.search)와 같은 규칙으로 정규화·이스케이프한다
    private List<String> search(String keyword) {
        return searchAll(keyword, 20).stream()
                .map(Competition::getName)
                .toList();
    }

    private List<Competition> searchAll(String keyword, int limit) {
        String normalized = Competition.normalize(keyword);
        String escaped = CompetitionService.escapeLike(normalized);
        return competitionRepository.search(normalized, escaped + "%", "%" + escaped + "%", 2026, PageRequest.of(0, limit));
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
}
