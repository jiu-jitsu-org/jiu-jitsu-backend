package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentLike;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 대회 이야기 쿼리 (반응 순·최신 순, 피드와 같은 제외 조건)와 게시글 ↔ 대회 연결
@DataJpaTest
class BoardCompetitionStoryRepositoryTest {

    private static final List<Long> NONE = List.of(-1L);   // 서비스와 같은 IN 절 sentinel
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 1, 12, 0);

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TestEntityManager em;

    private User author;
    private BoardCategory category;
    private Competition competition;
    private int minutes;

    @BeforeEach
    void setUp() {
        author = saveUser();
        category = em.persist(BoardCategory.builder().name("대회").isActive(true).build());
        competition = saveCompetition("서울 오픈");
    }

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

    // 부를 때마다 1분씩 늦게 쓴 글 (작성 시각을 고정해 최신순을 확정한다)
    private Board post(String title, Competition linked, User writer) {
        Board board = Board.builder()
                .category(category)
                .content(Content.builder().contentType(ContentType.BOARD).build())
                .competition(linked)
                .title(title)
                .body("본문")
                .build();
        ReflectionTestUtils.setField(board, "createdBy", writer);
        em.persist(board);
        em.flush();
        em.getEntityManager().createNativeQuery("UPDATE board SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, BASE.plusMinutes(minutes++))
                .setParameter(2, board.getId())
                .executeUpdate();
        return board;
    }

    private Board post(String title) {
        return post(title, competition, author);
    }

    private void comments(Board board, int count) {
        for (int i = 0; i < count; i++) {
            em.persist(CommunityComments.builder().content(board.getContent()).body("댓글").build());
        }
    }

    private void likes(Board board, int count) {
        for (int i = 0; i < count; i++) {
            ContentLike like = ContentLike.builder().content(board.getContent()).build();
            ReflectionTestUtils.setField(like, "createdBy", saveUser());
            em.persist(like);
        }
    }

    private List<String> byReaction(List<Long> excludedAuthors, List<Long> excludedContents, int limit) {
        em.flush();
        em.clear();
        return boardRepository.findCompetitionStoriesByReaction(competition.getId(), excludedAuthors, excludedContents, PageRequest.of(0, limit))
                .stream().map(Board::getTitle).toList();
    }

    private List<String> latest(List<Long> excludedAuthors, List<Long> excludedContents, int limit) {
        em.flush();
        em.clear();
        return boardRepository.findCompetitionStoriesLatest(competition.getId(), excludedAuthors, excludedContents, PageRequest.of(0, limit))
                .stream().map(Board::getTitle).toList();
    }

    @Test
    @DisplayName("반응 순: 댓글 수 + 좋아요 수가 많은 순, 같으면 최신 글 먼저, 반응 없는 글은 빠진다")
    void byReaction_order() {
        Board quiet = post("반응없음");
        Board commentsOnly = post("댓글3");
        Board likesOnly = post("좋아요2");
        Board mixed = post("댓글1좋아요2");
        comments(commentsOnly, 3);
        likes(likesOnly, 2);
        comments(mixed, 1);
        likes(mixed, 2);

        assertThat(byReaction(NONE, NONE, 10)).containsExactly("댓글1좋아요2", "댓글3", "좋아요2");
        assertThat(byReaction(NONE, NONE, 2)).containsExactly("댓글1좋아요2", "댓글3");
        assertThat(quiet.getId()).isNotNull();
    }

    @Test
    @DisplayName("반응 순: 숨김·삭제된 댓글은 반응으로 세지 않는다 (피드 댓글 수와 같은 집계)")
    void byReaction_ignoresHiddenAndDeletedComments() {
        Board board = post("숨김댓글만");
        CommunityComments hidden = CommunityComments.builder().content(board.getContent()).body("숨김").build();
        hidden.hide();
        em.persist(hidden);
        CommunityComments deleted = CommunityComments.builder().content(board.getContent()).body("삭제").build();
        ReflectionTestUtils.setField(deleted, "deletedAt", BASE);
        em.persist(deleted);

        assertThat(byReaction(NONE, NONE, 10)).isEmpty();
    }

    @Test
    @DisplayName("최신 순: 이 대회 글만 최신부터 limit 개")
    void latest_order() {
        post("첫글");
        post("둘째");
        post("다른대회", saveCompetition("부산 오픈"), author);
        post("연결없음", null, author);
        post("셋째");

        assertThat(latest(NONE, NONE, 10)).containsExactly("셋째", "둘째", "첫글");
        assertThat(latest(NONE, NONE, 2)).containsExactly("셋째", "둘째");
    }

    @Test
    @DisplayName("피드와 같은 제외: 숨김 글, 차단한 작성자, 신고·숨긴 글(제외 contentId)은 두 쿼리 모두에서 빠진다")
    void exclusions() {
        User blocked = saveUser();
        Board visible = post("보임");
        Board hidden = post("전체숨김");
        hidden.hide();
        Board fromBlocked = post("차단작성자", competition, blocked);
        Board reported = post("신고한글");
        for (Board board : List.of(visible, hidden, fromBlocked, reported)) {
            comments(board, 1);
        }

        List<Long> excludedAuthors = List.of(blocked.getId());
        List<Long> excludedContents = List.of(reported.getContent().getId());
        assertThat(latest(excludedAuthors, excludedContents, 10)).containsExactly("보임");
        assertThat(byReaction(excludedAuthors, excludedContents, 10)).containsExactly("보임");
    }

    @Test
    @DisplayName("연결 저장, 대회가 삭제되면 DB 가 연결을 비운다 (글은 남음)")
    void link_setNullOnCompetitionDelete() {
        Board board = post("연결글");
        em.clear();
        assertThat(boardRepository.findById(board.getId()).orElseThrow().getCompetitionId()).isEqualTo(competition.getId());

        em.getEntityManager().createNativeQuery("DELETE FROM competitions WHERE id = ?1")
                .setParameter(1, competition.getId())
                .executeUpdate();
        em.clear();

        assertThat(boardRepository.findById(board.getId()).orElseThrow().getCompetitionId()).isNull();
    }
}
