package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 게시글 목록 조건 — 카테고리 · 대회 · 피드 제외 (#269)
@DataJpaTest
class BoardListConditionRepositoryTest {

    private static final List<Long> NONE = List.of(-1L);
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 1, 12, 0);

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TestEntityManager em;

    private User author;
    private BoardCategory comp;
    private BoardCategory talk;
    private Competition seoul;
    private Competition gwangju;
    private int minutes;

    @BeforeEach
    void setUp() {
        author = em.persist(User.builder().snsProvider(SnsProvider.KAKAO).snsId(UUID.randomUUID().toString())
                .role(UserRole.USER).status(UserStatus.ACTIVE).build());
        comp = em.persist(BoardCategory.builder().name("대회").isActive(true).build());
        talk = em.persist(BoardCategory.builder().name("매트 위 수다").isActive(true).build());
        seoul = competition("서울 오픈");
        gwangju = competition("광주 챔피언십");
    }

    @Test
    @DisplayName("대회 조건: 그 대회를 고른 글만 최신순, 카테고리와 함께 써도 동작")
    void competitionAndCategory() {
        post("서울 질문", comp, seoul);
        post("서울 수다", talk, seoul);
        post("광주 질문", comp, gwangju);
        post("대회 없는 글", comp, null);
        Board hidden = post("서울 숨김", comp, seoul);
        hidden.hide();

        assertThat(titles(visible().and(BoardSpecifications.inCompetition(seoul.getId()))))
                .containsExactly("서울 수다", "서울 질문");
        assertThat(titles(visible().and(BoardSpecifications.inCategory(comp.getId())).and(BoardSpecifications.inCompetition(seoul.getId()))))
                .containsExactly("서울 질문");
        assertThat(titles(visible().and(BoardSpecifications.inCategory(comp.getId()))))
                .containsExactly("대회 없는 글", "광주 질문", "서울 질문");
        assertThat(titles(visible())).hasSize(4);
    }

    private Specification<Board> visible() {
        return BoardSpecifications.visibleInFeed(NONE, NONE);
    }

    private List<String> titles(Specification<Board> condition) {
        em.flush();
        em.clear();
        return boardRepository.findAll(condition, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(Board::getTitle).getContent();
    }

    private Competition competition(String name) {
        return em.persist(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(2026)
                .published(true)
                .openDate(LocalDate.of(2026, 12, 6))
                .build());
    }

    // 부를 때마다 1분씩 늦게 쓴 글 (작성 시각을 고정해 최신순을 확정한다)
    private Board post(String title, BoardCategory category, Competition linked) {
        Board board = Board.builder()
                .category(category)
                .content(Content.builder().contentType(ContentType.BOARD).build())
                .competition(linked)
                .title(title)
                .body("")
                .build();
        ReflectionTestUtils.setField(board, "createdBy", author);
        em.persist(board);
        em.flush();
        em.getEntityManager().createNativeQuery("UPDATE board SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, BASE.plusMinutes(minutes++))
                .setParameter(2, board.getId())
                .executeUpdate();
        return board;
    }
}
