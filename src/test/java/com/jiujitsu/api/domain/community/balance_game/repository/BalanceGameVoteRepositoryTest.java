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
}
