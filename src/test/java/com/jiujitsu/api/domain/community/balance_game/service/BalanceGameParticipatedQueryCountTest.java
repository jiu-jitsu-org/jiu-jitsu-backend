package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.security.AuditorAwareImpl;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 내가 투표한 목록이 봤음 여부(resultSeen)까지 판 수와 상관없이 같은 수의 쿼리로 나오는지 실제 매퍼까지 태워 본다. (#290)
 * 댓글 · 좋아요 · 알림 집계는 이 변경과 무관해 목으로 두고, 밸런스 게임 쪽 쿼리(투표 목록 · 득표수)만 센다.
 */
@DataJpaTest
@Import({BalanceGameService.class, BalanceGameMapper.class, AuditorAwareImpl.class})
class BalanceGameParticipatedQueryCountTest {

    @Autowired private BalanceGameService balanceGameService;
    @Autowired private TestEntityManager em;

    @MockBean private BalanceGameFactory balanceGameFactory;
    @MockBean private CommunityCommentsService communityCommentsService;
    @MockBean private ContentService contentService;
    @MockBean private NoticeService noticeService;
    @MockBean private AuthenticationFacade authenticationFacade;

    private User me;

    @BeforeEach
    void setUp() {
        me = em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        given(authenticationFacade.getCurrentUser()).willReturn(me);
    }

    @Test
    @DisplayName("판이 1개일 때와 3개일 때 쿼리 수가 같다 (투표 목록 1 + 득표수 1)")
    void queryCountDoesNotGrow() {
        voteClosedGame(1, true);
        assertThat(queriesFor(List.of(true))).isEqualTo(2);

        voteClosedGame(2, false);
        voteClosedGame(3, true);
        assertThat(queriesFor(List.of(true, false, true))).isEqualTo(2);
    }

    private void voteClosedGame(int daysAgo, boolean seen) {
        LocalDateTime endAt = LocalDateTime.now().minusDays(daysAgo);
        BalanceGame game = em.persist(BalanceGame.builder()
                .content(Content.builder().contentType(ContentType.BALANCE).build())
                .optionAText("A")
                .optionBText("B")
                .startAt(endAt.minusHours(1))
                .endAt(endAt)
                .gameDate(endAt.toLocalDate())
                .build());
        BalanceGameVote vote = BalanceGameVote.builder()
                .balanceGame(game)
                .option(BalanceGameOption.A)
                .resultSeenAt(seen ? LocalDateTime.now() : null)
                .build();
        ReflectionTestUtils.setField(vote, "createdBy", me);
        em.persist(vote);
    }

    private long queriesFor(List<Boolean> expectedResultSeen) {
        em.flush();
        em.clear();
        Statistics statistics = em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(balanceGameService.getParticipatedList(PageRequest.of(0, 20)).getContent())
                    .extracting(BalanceGameResponse::resultSeen)
                    .isEqualTo(expectedResultSeen);
            return statistics.getPrepareStatementCount();
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }
}
