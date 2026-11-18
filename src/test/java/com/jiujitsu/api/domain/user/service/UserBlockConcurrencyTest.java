package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserBlockRepository;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

/**
 * 같은 회원 차단이 동시에 들어와도(연타·두 기기) 500 없이 한 건만 저장되고 둘 다 차단 상태로 응답하는지 실제 트랜잭션으로 검증한다. (#232)
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(UserBlockService.class)
class UserBlockConcurrencyTest {

    @Autowired private UserBlockService userBlockService;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManager em;

    @SpyBean private UserBlockRepository userBlockRepository;

    @MockBean private AuthenticationFacade authenticationFacade;

    private User me;
    private User target;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(status -> {
            me = saveUser();
            target = saveUser();
        });
        given(authenticationFacade.getCurrentUser()).willReturn(me);
    }

    @Test
    @DisplayName("동시에 들어온 차단 두 번은 한 건만 저장되고, 둘 다 true (늦은 쪽의 유니크 위반을 차단됨으로 흡수)")
    void concurrentBlock_savesOnce() throws Exception {
        bothSeeNotBlockedBeforeWriting();

        List<Boolean> responses = runConcurrently(2);

        assertThat(responses).containsOnly(true);
        assertThat(blockRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 차단한 회원을 다시 차단해도 행이 그대로 남는다 (토글로 풀리지 않음)")
    void blockTwice_staysBlocked() {
        assertThat(userBlockService.block(target.getId())).isTrue();
        assertThat(userBlockService.block(target.getId())).isTrue();

        assertThat(blockRows()).isEqualTo(1);
    }

    // 두 요청이 모두 "차단 안 됨"을 읽은 뒤에야 저장으로 넘어가게 맞춘다 → 경합을 매번 재현
    private void bothSeeNotBlockedBeforeWriting() {
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger reads = new AtomicInteger();
        Answer<?> real = mockingDetails(userBlockRepository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            Object result = real.answer(invocation);
            if (reads.getAndIncrement() < 2) {
                barrier.await(5, TimeUnit.SECONDS);
            }
            return result;
        }).when(userBlockRepository).existsByBlockerIdAndBlockedId(any(), any());
    }

    private List<Boolean> runConcurrently(int requests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(pool.submit(() -> userBlockService.block(target.getId())));
            }
            List<Boolean> responses = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                responses.add(future.get(20, TimeUnit.SECONDS));   // 요청이 예외로 끝나면 여기서 실패
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }

    private User saveUser() {
        User user = User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        em.persist(user);
        return user;
    }

    private long blockRows() {
        return tx.execute(status -> em.createQuery(
                        "select count(ub) from UserBlock ub where ub.blocker.id = :blocker and ub.blocked.id = :blocked", Long.class)
                .setParameter("blocker", me.getId())
                .setParameter("blocked", target.getId())
                .getSingleResult());
    }
}
