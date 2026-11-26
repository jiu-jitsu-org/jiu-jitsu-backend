package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

/**
 * 같은 사람의 '결과 봤음'이 동시에 들어와도 오류 없이 한 번만 기록되는지 실제 트랜잭션으로 검증한다. (#290)
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import({BalanceGameService.class, BalanceGameMapper.class, AuditorAwareImpl.class})
class BalanceGameResultSeenConcurrencyTest {

    @Autowired private BalanceGameService balanceGameService;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManager em;

    @SpyBean private BalanceGameVoteRepository balanceGameVoteRepository;

    @MockBean private BalanceGameFactory balanceGameFactory;
    @MockBean private CommunityCommentsService communityCommentsService;
    @MockBean private ContentService contentService;
    @MockBean private NoticeService noticeService;
    @MockBean private AuthenticationFacade authenticationFacade;

    private User me;
    private Long contentId;
    private Long voteId;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(status -> {
            me = User.builder()
                    .snsProvider(SnsProvider.KAKAO)
                    .snsId(UUID.randomUUID().toString())
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            em.persist(me);
            LocalDateTime now = LocalDateTime.now();
            BalanceGame game = BalanceGame.builder()
                    .content(Content.builder().contentType(ContentType.BALANCE).build())
                    .optionAText("A")
                    .optionBText("B")
                    .startAt(now.minusDays(1))
                    .endAt(now.minusHours(1))
                    .gameDate(now.minusDays(1).toLocalDate())
                    .build();
            em.persist(game);
            contentId = game.getContent().getId();
            BalanceGameVote vote = BalanceGameVote.builder().balanceGame(game).option(BalanceGameOption.A).build();
            ReflectionTestUtils.setField(vote, "createdBy", me);
            em.persist(vote);
            voteId = vote.getId();
        });
        given(authenticationFacade.getCurrentUser()).willReturn(me);
    }

    @Test
    @DisplayName("동시에 들어온 두 번 중 한 번만 기록되고 둘 다 오류 없이 끝난다. 뒤에 다시 불러도 처음 시각 그대로")
    void concurrent_marksOnce() throws Exception {
        List<Integer> updatedRows = bothReachUpdateTogether();

        runConcurrently(2);

        assertThat(updatedRows).containsExactlyInAnyOrder(1, 0);
        LocalDateTime first = seenAt();
        assertThat(first).isNotNull();

        balanceGameService.markResultSeen(contentId);

        assertThat(updatedRows).hasSize(3).endsWith(0);
        assertThat(seenAt()).isEqualTo(first);
    }

    // 두 요청이 모두 기록 직전까지 온 뒤에야 쓰기로 넘어가게 맞춘다 → 경합을 매번 재현
    private List<Integer> bothReachUpdateTogether() {
        List<Integer> updatedRows = new CopyOnWriteArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(2);
        // 리포지토리는 인터페이스 프록시라 callRealMethod 를 못 쓴다 → 실제 빈으로 넘기는 기본 Answer 에 위임
        Answer<?> real = mockingDetails(balanceGameVoteRepository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            if (updatedRows.size() < 2) {   // 처음 두 요청만 맞춘다
                barrier.await(5, TimeUnit.SECONDS);
            }
            Integer rows = (Integer) real.answer(invocation);
            updatedRows.add(rows);
            return rows;
        }).when(balanceGameVoteRepository).markResultSeen(any(), any(), any());
        return updatedRows;
    }

    private void runConcurrently(int requests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(pool.submit(() -> balanceGameService.markResultSeen(contentId)));
            }
            for (Future<?> future : futures) {
                future.get(20, TimeUnit.SECONDS);   // 요청이 예외로 끝나면 여기서 실패
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private LocalDateTime seenAt() {
        return tx.execute(status -> em.find(BalanceGameVote.class, voteId).getResultSeenAt());
    }
}
