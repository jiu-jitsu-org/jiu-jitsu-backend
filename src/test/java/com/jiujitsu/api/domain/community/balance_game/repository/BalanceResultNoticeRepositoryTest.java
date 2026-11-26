package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.notice.entity.ContentNoticeSetting;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.global.security.AuditorAwareImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 결과 알림 대상 판 · 받는 사람 · 보낸 기록 쿼리 (#291)
@DataJpaTest
@Import(AuditorAwareImpl.class)
class BalanceResultNoticeRepositoryTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 11, 25, 0, 0);
    private static final LocalDateTime TO = FROM.plusDays(1);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 25, 23, 59, 59);
    private static final LocalDateTime NOON = LocalDateTime.of(2026, 11, 26, 11, 55);

    @Autowired private BalanceGameRepository balanceGameRepository;
    @Autowired private BalanceGameVoteRepository balanceGameVoteRepository;
    @Autowired private TestEntityManager em;

    private User user(UserStatus status) {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(status)
                .build());
    }

    private BalanceGame game(LocalDateTime endAt) {
        return em.persist(BalanceGame.builder()
                .content(Content.builder().contentType(ContentType.BALANCE).build())
                .optionAText("A")
                .optionBText("B")
                .startAt(endAt.minusDays(1))
                .endAt(endAt)
                .gameDate(endAt.toLocalDate())
                .build());
    }

    private BalanceGameVote vote(BalanceGame game, User voter) {
        BalanceGameVote vote = BalanceGameVote.builder().balanceGame(game).option(BalanceGameOption.A).build();
        ReflectionTestUtils.setField(vote, "createdBy", voter);
        return em.persist(vote);
    }

    private BalanceGame gameWithVotes(LocalDateTime endAt, int votes) {
        BalanceGame game = game(endAt);
        for (int i = 0; i < votes; i++) {
            vote(game, user(UserStatus.ACTIVE));
        }
        return game;
    }

    private List<Long> due(long minVotes) {
        em.flush();
        em.clear();
        return balanceGameRepository.findClosedBetweenWithMinVotes(FROM, TO, minVotes).stream().map(BalanceGame::getId).toList();
    }

    private List<Long> targetUserIds(BalanceGame game) {
        em.flush();
        em.clear();
        return balanceGameVoteRepository.findResultNoticeTargets(game.getId()).stream()
                .map(BalanceGameVoteRepository.ResultNoticeTarget::getUserId).toList();
    }

    @Test
    @DisplayName("대상 판: 전날 00:00:00 ~ 23:59:59 에 마감된 판만 (그 전날 · 오늘 00:00 마감은 제외)")
    void dueGames_byEndAt() {
        BalanceGame start = gameWithVotes(FROM, 5);
        BalanceGame end = gameWithVotes(END, 5);
        gameWithVotes(FROM.minusSeconds(1), 5);
        gameWithVotes(TO, 5);

        assertThat(due(5)).containsExactly(start.getId(), end.getId());
    }

    @Test
    @DisplayName("대상 판: 마감 날짜로 본다 — 날짜를 넘겨 진행한 판은 진행일이 이틀 전이어도 대상")
    void dueGames_notByGameDate() {
        BalanceGame game = gameWithVotes(END, 5);
        ReflectionTestUtils.setField(game, "gameDate", FROM.toLocalDate().minusDays(1));

        assertThat(due(5)).containsExactly(game.getId());
    }

    @Test
    @DisplayName("대상 판: 총 투표 5명 이상 (4명 제외). 탈퇴해 사라진 사람의 표도 화면의 총 투표처럼 센다")
    void dueGames_minVotes() {
        gameWithVotes(END, 4);
        BalanceGame five = gameWithVotes(END, 5);
        BalanceGame withDeletedVoter = gameWithVotes(END, 4);
        vote(withDeletedVoter, null);

        assertThat(due(5)).containsExactly(five.getId(), withDeletedVoter.getId());
    }

    @Test
    @DisplayName("받는 사람: 그 판에 투표하고 결과를 안 본 사람 — 본 사람 · 이미 보낸 사람 · 종 끈 사람 · 탈퇴 · 다른 판 투표자 제외")
    void targets() {
        BalanceGame game = game(END);
        BalanceGame other = game(END);
        User ok = user(UserStatus.ACTIVE);
        User seen = user(UserStatus.ACTIVE);
        User noticed = user(UserStatus.ACTIVE);
        User bellOff = user(UserStatus.ACTIVE);
        User bellOn = user(UserStatus.ACTIVE);
        User bellOffOtherGame = user(UserStatus.ACTIVE);
        User deleted = user(UserStatus.DELETED);
        User otherVoter = user(UserStatus.ACTIVE);
        for (User voter : List.of(ok, bellOff, bellOn, bellOffOtherGame, deleted)) {
            vote(game, voter);
        }
        vote(game, null);   // 탈퇴 뒤 행까지 지워진 사람
        vote(other, otherVoter);
        BalanceGameVote noticedVote = vote(game, noticed);
        vote(game, seen);
        em.persist(setting(bellOff, game, false));
        em.persist(setting(bellOn, game, true));
        em.persist(setting(bellOffOtherGame, other, false));
        em.flush();
        balanceGameVoteRepository.markResultSeen(game.getId(), seen, NOON);
        balanceGameVoteRepository.markResultNoticed(noticedVote.getId(), NOON);

        assertThat(targetUserIds(game)).containsExactly(ok.getId(), bellOn.getId(), bellOffOtherGame.getId());
    }

    private static ContentNoticeSetting setting(User user, BalanceGame game, boolean enabled) {
        return ContentNoticeSetting.builder().userId(user.getId()).contentId(game.getContent().getId()).enabled(enabled).build();
    }

    @Test
    @DisplayName("받는 사람: 투표를 취소하면(행 삭제) 빠진다")
    void targets_cancelled() {
        BalanceGame game = game(END);
        User kept = user(UserStatus.ACTIVE);
        vote(game, kept);
        BalanceGameVote cancelled = vote(game, user(UserStatus.ACTIVE));
        balanceGameVoteRepository.delete(cancelled);

        assertThat(targetUserIds(game)).containsExactly(kept.getId());
    }

    @Test
    @DisplayName("받는 사람: 알림 설정(커뮤니티 활동 · 결과 알림)을 꺼도 알림함 대상에는 남는다 (푸시에서만 거름)")
    void targets_settingOff() {
        BalanceGame game = game(END);
        User off = user(UserStatus.ACTIVE);
        UserNoticeSetting setting = new UserNoticeSetting(off, false);
        ReflectionTestUtils.setField(setting, "communityEnabled", false);
        ReflectionTestUtils.setField(setting, "balanceResultEnabled", false);
        em.persist(setting);
        vote(game, off);

        assertThat(targetUserIds(game)).containsExactly(off.getId());
    }

    @Test
    @DisplayName("보낸 기록은 한 번만 남는다 — 다시 부르면 0행, 시각도 그대로")
    void markNoticed_once() {
        BalanceGameVote vote = vote(game(END), user(UserStatus.ACTIVE));
        em.flush();

        assertThat(balanceGameVoteRepository.markResultNoticed(vote.getId(), NOON)).isEqualTo(1);
        assertThat(balanceGameVoteRepository.markResultNoticed(vote.getId(), NOON.plusMinutes(1))).isZero();
        em.clear();
        assertThat(em.find(BalanceGameVote.class, vote.getId()).getResultNoticedAt()).isEqualTo(NOON);
    }

    @Test
    @DisplayName("받을 사람을 뽑은 뒤 결과를 봤으면 보낸 기록을 남기지 않는다 (0행)")
    void markNoticed_seenMeanwhile() {
        BalanceGame game = game(END);
        User voter = user(UserStatus.ACTIVE);
        BalanceGameVote vote = vote(game, voter);
        em.flush();
        balanceGameVoteRepository.markResultSeen(game.getId(), voter, NOON);

        assertThat(balanceGameVoteRepository.markResultNoticed(vote.getId(), NOON)).isZero();
    }
}
