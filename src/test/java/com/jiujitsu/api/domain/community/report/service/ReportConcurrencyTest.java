package com.jiujitsu.api.domain.community.report.service;

import com.jiujitsu.api.domain.community.report.dto.ReportCreateRequest;
import com.jiujitsu.api.domain.community.report.entity.ReportReason;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.mapper.ReportMapper;
import com.jiujitsu.api.domain.community.report.repository.ReportRepository;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.security.AuditorAwareImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * 같은 회원 신고가 동시에 들어와도 500 없이 한 건만 저장되고, 늦은 쪽은 중복 신고(U0008)로 응답하는지 실제 트랜잭션으로 검증한다. (#232)
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import({ReportService.class, ReportMapper.class, AuditorAwareImpl.class})
class ReportConcurrencyTest {

    @Autowired private ReportService reportService;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManager em;

    @SpyBean private ReportRepository reportRepository;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("동시에 들어온 회원 신고 두 번은 한 건만 저장되고, 하나는 성공 하나는 U0008 (500 없음)")
    void concurrentUserReport_savesOnce() throws Exception {
        bothSeeNotReportedBeforeWriting();

        List<Object> outcomes = runConcurrently(2);

        assertThat(outcomes).containsExactlyInAnyOrder("OK", ErrorCode.ALREADY_REPORTED);
        assertThat(reportRows()).isEqualTo(1);
    }

    private void bothSeeNotReportedBeforeWriting() {
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger reads = new AtomicInteger();
        Answer<?> real = mockingDetails(reportRepository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            Object result = real.answer(invocation);
            if (reads.getAndIncrement() < 2) {
                barrier.await(5, TimeUnit.SECONDS);
            }
            return result;
        }).when(reportRepository).existsByCreatedByAndReportTypeAndTargetId(any(), any(), any());
    }

    private List<Object> runConcurrently(int requests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(pool.submit(this::reportAsMe));
            }
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get(20, TimeUnit.SECONDS));   // ErrorException 이 아닌 예외면 여기서 실패
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    // 감사(@CreatedBy = 신고자)가 로그인 유저로 채워지도록 요청 스레드에 인증을 둔다
    private Object reportAsMe() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(me.getId(), null, List.of()));
        try {
            reportService.createReport(new ReportCreateRequest(ReportType.USER, target.getId(), ReportReason.ABUSE, null));
            return "OK";
        } catch (ErrorException e) {
            return e.getErrorCode();
        } finally {
            SecurityContextHolder.clearContext();
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

    private long reportRows() {
        return tx.execute(status -> em.createQuery(
                        "select count(r) from Report r where r.reportType = :type and r.targetId = :target", Long.class)
                .setParameter("type", ReportType.USER)
                .setParameter("target", target.getId())
                .getSingleResult());
    }
}
