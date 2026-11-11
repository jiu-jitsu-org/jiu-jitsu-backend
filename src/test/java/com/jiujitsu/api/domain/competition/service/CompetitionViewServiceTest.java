package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.CompetitionViewResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 원자 UPDATE 쿼리와 함께 검증한다
@DataJpaTest
@Import(CompetitionViewService.class)
class CompetitionViewServiceTest {

    @Autowired
    private CompetitionViewService competitionViewService;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("부를 때마다 조회수가 1씩 오르고, 올린 뒤 값을 돌려준다")
    void increase_everyCall() {
        Long id = em.persist(Competition.builder()
                .name("서울 오픈").searchName("서울오픈").competitionYear(2026).published(true)
                .openDate(LocalDate.of(2026, 12, 6)).build()).getId();
        em.flush();

        assertThat(competitionViewService.increase(id)).isEqualTo(new CompetitionViewResponse(id, 1));
        assertThat(competitionViewService.increase(id)).isEqualTo(new CompetitionViewResponse(id, 2));
        assertThat(competitionViewService.increase(id).viewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("없거나 미공개인 대회는 COMPETITION_NOT_FOUND")
    void increase_notFound() {
        Long catalog = em.persist(Competition.of("카탈로그", 2026)).getId();
        em.flush();

        assertThatThrownBy(() -> competitionViewService.increase(catalog))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        assertThatThrownBy(() -> competitionViewService.increase(-1L))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
    }
}
