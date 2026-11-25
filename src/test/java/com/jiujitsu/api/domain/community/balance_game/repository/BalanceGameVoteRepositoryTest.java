package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.global.security.AuditorAwareImpl;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AuditorAwareImpl.class)  // 투표자 = 감사 필드 created_by 라서 실제 감사 로직으로 채운다
class BalanceGameVoteRepositoryTest {

    @Autowired
    private BalanceGameVoteRepository balanceGameVoteRepository;

    @Autowired
    private TestEntityManager em;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private User saveUser() {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private BalanceGame saveGame(LocalDate gameDate, LocalDateTime endAt) {
        Content content = Content.builder()
                .contentType(ContentType.BALANCE)
                .build();
        return em.persist(BalanceGame.builder()
                .content(content)
                .optionAText("A")
                .optionBText("B")
                .endAt(endAt)
                .startAt(LocalDateTime.of(2000, 1, 1, 0, 0))
                .gameDate(gameDate)
                .build());
    }

    // AuditorAwareImpl 은 principal 이 userId(Long) 인 인증을 투표자로 기록한다.
    private BalanceGameVote vote(User voter, BalanceGame game, BalanceGameOption option) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(voter.getId(), null, List.of()));
        BalanceGameVote vote = balanceGameVoteRepository.save(
                BalanceGameVote.builder().balanceGame(game).option(option).build());
        SecurityContextHolder.clearContext();
        return vote;
    }

    @Test
    @DisplayName("내가 투표한 게임만 gameDate 최신순으로 반환하고, 마감된 게임도 포함한다")
    void findMyVotes() {
        // given
        User me = saveUser();
        User other = saveUser();
        LocalDate today = LocalDate.now();
        BalanceGame closed = saveGame(today.minusDays(2), LocalDateTime.now().minusDays(2));
        BalanceGame current = saveGame(today, LocalDateTime.now().plusHours(1));
        BalanceGame yesterday = saveGame(today.minusDays(1), LocalDateTime.now().minusDays(1));
        BalanceGame othersOnly = saveGame(today.minusDays(3), LocalDateTime.now().minusDays(3));

        vote(me, closed, BalanceGameOption.A);
        vote(me, current, BalanceGameOption.B);
        vote(me, yesterday, BalanceGameOption.A);
        vote(other, current, BalanceGameOption.A);
        vote(other, othersOnly, BalanceGameOption.B);
        em.flush();
        em.clear();

        // when
        Page<BalanceGameVote> result = balanceGameVoteRepository.findMyVotes(me, PageRequest.of(0, 20));

        // then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(v -> v.getBalanceGame().getId())
                .containsExactly(current.getId(), yesterday.getId(), closed.getId());
        assertThat(result.getContent())
                .extracting(BalanceGameVote::getOption)
                .containsExactly(BalanceGameOption.B, BalanceGameOption.A, BalanceGameOption.A);
    }

    @Test
    @DisplayName("투표를 취소한(행 삭제) 게임은 목록에서 빠진다")
    void findMyVotes_excludesCancelled() {
        // given
        User me = saveUser();
        BalanceGame kept = saveGame(LocalDate.now(), LocalDateTime.now().plusHours(1));
        BalanceGame cancelled = saveGame(LocalDate.now().minusDays(1), LocalDateTime.now().minusDays(1));
        vote(me, kept, BalanceGameOption.A);
        BalanceGameVote toCancel = vote(me, cancelled, BalanceGameOption.B);
        balanceGameVoteRepository.delete(toCancel);
        em.flush();
        em.clear();

        // when
        Page<BalanceGameVote> result = balanceGameVoteRepository.findMyVotes(me, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent())
                .extracting(v -> v.getBalanceGame().getId())
                .containsExactly(kept.getId());
    }

    @Test
    @DisplayName("페이지 크기만큼 잘라 반환하고 전체 개수는 유지한다")
    void findMyVotes_paging() {
        // given
        User me = saveUser();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            vote(me, saveGame(today.minusDays(i), LocalDateTime.now().minusDays(i)), BalanceGameOption.A);
        }
        em.flush();
        em.clear();

        // when
        Page<BalanceGameVote> result = balanceGameVoteRepository.findMyVotes(me, PageRequest.of(1, 2));

        // then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBalanceGame().getGameDate()).isEqualTo(today.minusDays(2));
    }

    @Test
    @DisplayName("게임 다건의 선택지별 득표수를 집계한다")
    void countByBalanceGameIdsGroupByOption() {
        // given
        User u1 = saveUser();
        User u2 = saveUser();
        User u3 = saveUser();
        BalanceGame g1 = saveGame(LocalDate.now(), LocalDateTime.now().plusHours(1));
        BalanceGame g2 = saveGame(LocalDate.now().minusDays(1), LocalDateTime.now().minusDays(1));
        vote(u1, g1, BalanceGameOption.A);
        vote(u2, g1, BalanceGameOption.A);
        vote(u3, g1, BalanceGameOption.B);
        vote(u1, g2, BalanceGameOption.B);
        em.flush();
        em.clear();

        // when
        List<Object[]> rows = balanceGameVoteRepository.countByBalanceGameIdsGroupByOption(List.of(g1.getId(), g2.getId()));

        // then
        assertThat(rows)
                .extracting(row -> List.of(row[0], row[1], ((Number) row[2]).longValue()))
                .containsExactlyInAnyOrder(
                        List.of(g1.getId(), BalanceGameOption.A, 2L),
                        List.of(g1.getId(), BalanceGameOption.B, 1L),
                        List.of(g2.getId(), BalanceGameOption.B, 1L));
    }

    @Test
    @DisplayName("결과 봤음은 내 행에만 남고, 다시 불러도 처음 시각 그대로다 — 남의 투표·다른 판은 건드리지 않는다 (#290)")
    void markResultSeen() {
        // given
        User me = saveUser();
        User other = saveUser();
        BalanceGame game = saveGame(LocalDate.now().minusDays(1), LocalDateTime.now().minusDays(1));
        BalanceGame anotherGame = saveGame(LocalDate.now().minusDays(2), LocalDateTime.now().minusDays(2));
        BalanceGameVote mine = vote(me, game, BalanceGameOption.A);
        BalanceGameVote others = vote(other, game, BalanceGameOption.B);
        BalanceGameVote mineOnAnother = vote(me, anotherGame, BalanceGameOption.A);
        em.flush();
        em.clear();
        LocalDateTime first = LocalDateTime.of(2026, 11, 26, 9, 0);

        // when
        int marked = balanceGameVoteRepository.markResultSeen(game.getId(), me, first);
        int again = balanceGameVoteRepository.markResultSeen(game.getId(), me, first.plusHours(1));
        em.clear();

        // then
        assertThat(marked).isEqualTo(1);
        assertThat(again).isZero();
        assertThat(em.find(BalanceGameVote.class, mine.getId()).getResultSeenAt()).isEqualTo(first);
        assertThat(em.find(BalanceGameVote.class, others.getId()).isResultSeen()).isFalse();
        assertThat(em.find(BalanceGameVote.class, mineOnAnother.getId()).isResultSeen()).isFalse();
    }

    @Test
    @DisplayName("투표하지 않은 판에는 결과 봤음이 남지 않는다 (0행)")
    void markResultSeen_notVoted() {
        // given
        User me = saveUser();
        User other = saveUser();
        BalanceGame game = saveGame(LocalDate.now().minusDays(1), LocalDateTime.now().minusDays(1));
        BalanceGameVote others = vote(other, game, BalanceGameOption.B);
        em.flush();
        em.clear();

        // when
        int marked = balanceGameVoteRepository.markResultSeen(game.getId(), me, LocalDateTime.now());
        em.clear();

        // then
        assertThat(marked).isZero();
        assertThat(em.find(BalanceGameVote.class, others.getId()).isResultSeen()).isFalse();
    }

    @Test
    @DisplayName("내가 투표한 목록은 판이 여러 개여도 봤음 여부까지 조회 한 번으로 읽는다 (#290)")
    void findMyVotes_resultSeenWithoutExtraQuery() {
        // given
        User me = saveUser();
        LocalDate today = LocalDate.now();
        BalanceGame seen = saveGame(today.minusDays(1), LocalDateTime.now().minusDays(1));
        BalanceGame unseen = saveGame(today.minusDays(2), LocalDateTime.now().minusDays(2));
        vote(me, seen, BalanceGameOption.A);
        vote(me, unseen, BalanceGameOption.B);
        em.flush();
        balanceGameVoteRepository.markResultSeen(seen.getId(), me, LocalDateTime.now());
        em.clear();
        Statistics statistics = em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        // when
        List<Boolean> resultSeen = balanceGameVoteRepository.findMyVotes(me, PageRequest.of(0, 20)).getContent().stream()
                .map(BalanceGameVote::isResultSeen)
                .toList();
        long queries = statistics.getPrepareStatementCount();
        statistics.setStatisticsEnabled(false);

        // then
        assertThat(resultSeen).containsExactly(true, false);
        assertThat(queries).isEqualTo(1);
    }
}
