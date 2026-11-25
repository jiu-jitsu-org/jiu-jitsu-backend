package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceResultNoticeRunResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.factory.NoticeFactory;
import com.jiujitsu.api.domain.notice.repository.NoticeRepository;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import com.jiujitsu.api.global.fcm.event.FcmPushRequestedEvent;
import com.jiujitsu.api.global.properties.BalanceResultNoticeProperties;
import com.jiujitsu.api.global.security.AuditorAwareImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;

/**
 * 결과 알림을 실제 트랜잭션으로: 동시에 · 다시 돌려도 한 번, 판 하나가 실패하면 그 판만 되돌아가고 푸시는 커밋 뒤에만 (#291)
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import({BalanceResultNoticeService.class, BalanceResultNoticeRunService.class, FcmPushEventPublisher.class, NoticeFactory.class,
        AuditorAwareImpl.class, BalanceResultNoticeTransactionTest.Config.class})
class BalanceResultNoticeTransactionTest {

    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 25, 23, 59, 59);
    private static final int VOTERS = 5;

    @TestConfiguration
    static class Config {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-26T02:55:00Z"), ZoneId.of("Asia/Seoul"));   // 서울 11/26 11:55
        }

        @Bean
        BalanceResultNoticeProperties balanceResultNoticeProperties() {
            return new BalanceResultNoticeProperties(VOTERS);
        }

        @Bean
        CommittedPushes committedPushes() {
            return new CommittedPushes();
        }
    }

    // 실제 푸시 리스너(FcmPushEventListener)와 같은 시점 — 커밋된 것만 쌓인다
    static class CommittedPushes {
        final List<FcmPushRequestedEvent> events = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void on(FcmPushRequestedEvent event) {
            events.add(event);
        }
    }

    @Autowired private BalanceResultNoticeRunService runService;
    @Autowired private CommittedPushes pushes;
    @Autowired private NoticeRepository noticeRepository;
    @Autowired private NoticeFactory noticeFactory;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManager em;

    @SpyBean private BalanceGameVoteRepository balanceGameVoteRepository;
    @MockBean private NoticeService noticeService;

    @BeforeEach
    void saveNoticeForReal() {
        // 저장만 실제로 (NoticeService 의 나머지 의존성은 이 테스트와 무관)
        doAnswer(invocation -> noticeRepository.save(noticeFactory.createNotice(invocation.getArgument(0), invocation.getArgument(1),
                invocation.<Map<String, String>>getArgument(2), null, null, invocation.getArgument(3), invocation.getArgument(4))))
                .when(noticeService).saveNoticeWithText(anyLong(), any(), any(), anyString(), anyString());
    }

    private record Saved(Long contentId, List<Long> voterIds) {
    }

    private Saved saveGame(String title) {
        return tx.execute(status -> {
            BalanceGame game = BalanceGame.builder()
                    .content(Content.builder().contentType(ContentType.BALANCE).build())
                    .title(title)
                    .optionAText("매번 서브미션패")
                    .optionBText("매번 포인트패")
                    .startAt(END.minusHours(23))
                    .endAt(END)
                    .gameDate(END.toLocalDate())
                    .build();
            em.persist(game);
            List<Long> voterIds = new ArrayList<>();
            for (int i = 0; i < VOTERS; i++) {
                User voter = User.builder()
                        .snsProvider(SnsProvider.KAKAO)
                        .snsId(UUID.randomUUID().toString())
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build();
                em.persist(voter);
                BalanceGameVote vote = BalanceGameVote.builder().balanceGame(game).option(BalanceGameOption.A).build();
                ReflectionTestUtils.setField(vote, "createdBy", voter);
                em.persist(vote);
                voterIds.add(voter.getId());
            }
            return new Saved(game.getContent().getId(), voterIds);
        });
    }

    private List<Notice> notices(Saved game) {
        return noticeRepository.findAll().stream().filter(n -> n.getData().equals(String.valueOf(game.contentId()))).toList();
    }

    @Test
    @DisplayName("한 번 돌리면 투표자마다 한 건, 알림함과 푸시 문구가 같다. 다시 돌리면 0건")
    void runTwice() {
        Saved game = saveGame("더 싫은 패배는?");

        assertThat(runService.runYesterday()).isEqualTo(new BalanceResultNoticeRunResponse(END.toLocalDate(), 1, VOTERS, 0));
        assertThat(runService.runYesterday()).isEqualTo(new BalanceResultNoticeRunResponse(END.toLocalDate(), 1, 0, 0));

        assertThat(notices(game)).extracting(Notice::getUserId).containsExactlyInAnyOrderElementsOf(game.voterIds());
        assertThat(notices(game)).allSatisfy(n -> {
            assertThat(n.getTitle()).isEqualTo("투표 결과: 더 싫은 패배는?");
            assertThat(n.getBody()).isEqualTo("매번 서브미션패 vs 매번 포인트패");
            assertThat(n.getIsRead()).isFalse();
        });
        assertThat(pushes.events).hasSize(VOTERS).allSatisfy(push -> {
            assertThat(push.title()).isEqualTo("투표 결과: 더 싫은 패배는?");
            assertThat(push.body()).isEqualTo("매번 서브미션패 vs 매번 포인트패");
            assertThat(push.data()).isEqualTo(Map.of("type", "BALANCE_DETAIL", "data", String.valueOf(game.contentId())));
        });
    }

    @Test
    @DisplayName("두 서버가 같은 대상을 뽑은 채 동시에 돌아도 한 사람에 한 번, 오류 없이 끝난다")
    void concurrentRuns() throws Exception {
        Saved game = saveGame("타이틀");
        bothReadTargetsBeforeWriting();

        List<BalanceResultNoticeRunResponse> results = runConcurrently(2);

        assertThat(results).extracting(BalanceResultNoticeRunResponse::failed).containsOnly(0);
        assertThat(results.stream().mapToInt(BalanceResultNoticeRunResponse::sent).sum()).isEqualTo(VOTERS);
        assertThat(notices(game)).extracting(Notice::getUserId).containsExactlyInAnyOrderElementsOf(game.voterIds());
        assertThat(pushes.events).extracting(FcmPushRequestedEvent::recipientUserId).containsExactlyInAnyOrderElementsOf(game.voterIds());
    }

    @Test
    @DisplayName("판 하나가 중간에 실패하면 그 판은 기록 · 알림 · 푸시가 모두 되돌아가고 나머지 판은 간다. 다시 돌리면 실패한 판만 간다")
    void oneGameFails() {
        Saved failing = saveGame("실패하는 판");
        Saved ok = saveGame("되는 판");
        doThrow(new IllegalStateException("저장 실패")).when(noticeService)
                .saveNoticeWithText(eq(failing.voterIds().get(VOTERS - 1)), any(), any(), anyString(), anyString());

        assertThat(runService.runYesterday()).isEqualTo(new BalanceResultNoticeRunResponse(END.toLocalDate(), 2, VOTERS, 1));
        assertThat(notices(failing)).isEmpty();
        assertThat(notices(ok)).hasSize(VOTERS);
        assertThat(pushes.events).extracting(FcmPushRequestedEvent::recipientUserId).containsExactlyInAnyOrderElementsOf(ok.voterIds());

        saveNoticeForReal();
        assertThat(runService.runYesterday()).isEqualTo(new BalanceResultNoticeRunResponse(END.toLocalDate(), 2, VOTERS, 0));
        assertThat(notices(failing)).hasSize(VOTERS);
        assertThat(notices(ok)).hasSize(VOTERS);
    }

    // 두 실행이 모두 같은 대상을 읽은 뒤에야 쓰기로 넘어가게 맞춘다 → 경합을 매번 재현
    private void bothReadTargetsBeforeWriting() {
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger calls = new AtomicInteger();
        // 리포지토리는 인터페이스 프록시라 callRealMethod 를 못 쓴다 → 실제 빈으로 넘기는 기본 Answer 에 위임
        Answer<?> real = mockingDetails(balanceGameVoteRepository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            Object targets = real.answer(invocation);
            if (calls.incrementAndGet() <= 2) {
                barrier.await(5, TimeUnit.SECONDS);
            }
            return targets;
        }).when(balanceGameVoteRepository).findResultNoticeTargets(anyLong());
    }

    private List<BalanceResultNoticeRunResponse> runConcurrently(int servers) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(servers);
        try {
            List<Future<BalanceResultNoticeRunResponse>> futures = new ArrayList<>();
            for (int i = 0; i < servers; i++) {
                futures.add(pool.submit(runService::runYesterday));
            }
            List<BalanceResultNoticeRunResponse> results = new ArrayList<>();
            for (Future<BalanceResultNoticeRunResponse> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
