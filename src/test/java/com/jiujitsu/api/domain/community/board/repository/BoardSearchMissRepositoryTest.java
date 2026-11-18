package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissResponse;
import com.jiujitsu.api.domain.community.board.entity.BoardSearchMiss;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 결과 0건 검색어 집계 — 기간 안, 많이 나온 순 · 같으면 최근 순 (#266)
@DataJpaTest
class BoardSearchMissRepositoryTest {

    private static final LocalDateTime DAY = LocalDateTime.of(2026, 12, 1, 0, 0);

    @Autowired
    private BoardSearchMissRepository boardSearchMissRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("기간 안의 검색어를 횟수 순(같으면 최근 순)으로, 기간 밖은 빠지고 limit 만큼")
    void topKeywords() {
        miss("베림보로", DAY.plusHours(1));
        miss("베림보로", DAY.plusHours(2));
        miss("베림보로", DAY.plusHours(3));
        miss("데라히바", DAY.plusHours(4));
        miss("데라히바", DAY.plusHours(5));
        miss("nogi 부산", DAY.plusHours(6));
        miss("주짓수 화성", DAY.plusHours(1));
        miss("기간 밖", DAY.minusSeconds(1));
        miss("기간 밖", DAY.plusDays(1));
        em.flush();

        List<AdminSearchMissResponse> result = boardSearchMissRepository.findTopKeywords(DAY, DAY.plusDays(1), PageRequest.of(0, 10));

        assertThat(result).extracting(AdminSearchMissResponse::keyword).containsExactly("베림보로", "데라히바", "nogi 부산", "주짓수 화성");
        assertThat(result.get(0).count()).isEqualTo(3);
        assertThat(result.get(0).lastSearchedAt()).isEqualTo(DAY.plusHours(3));
        assertThat(boardSearchMissRepository.findTopKeywords(DAY, DAY.plusDays(1), PageRequest.of(0, 2))).hasSize(2);
    }

    private void miss(String keyword, LocalDateTime at) {
        em.persist(BoardSearchMiss.builder().keyword(keyword).searchedAt(at).build());
    }
}
