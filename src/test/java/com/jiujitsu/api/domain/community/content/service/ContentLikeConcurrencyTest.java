package com.jiujitsu.api.domain.community.content.service;

import com.jiujitsu.api.domain.community.content.dto.ContentLikeResponse;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentLike;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.mapper.ContentLikeMapper;
import com.jiujitsu.api.domain.community.content.mapper.ContentSaveMapper;
import com.jiujitsu.api.domain.community.content.repository.ContentLikeRepository;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
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
import org.springframework.test.util.ReflectionTestUtils;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 같은 사람의 게시글 좋아요가 동시에 들어와도 500 없이 한 건만 저장되는지 실제 트랜잭션으로 검증한다. (#199)
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import({ContentService.class, ContentLikeMapper.class, ContentSaveMapper.class, AuditorAwareImpl.class})
class ContentLikeConcurrencyTest {

    @Autowired private ContentService contentService;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManager em;

    @SpyBean private ContentLikeRepository contentLikeRepository;

    @MockBean private AuthenticationFacade authenticationFacade;
    @MockBean private FcmPushEventPublisher fcmPushEventPublisher;
    @MockBean private NoticeService noticeService;

    private User author;
    private User me;
    private Long contentId;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(status -> {
            author = saveUser();
            me = saveUser();
            Content content = Content.builder().contentType(ContentType.BOARD).build();
            ReflectionTestUtils.setField(content, "createdBy", author);
            em.persist(content);
            contentId = content.getId();
        });
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(noticeService.isContentNoticeEnabled(any(), any())).willReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("동시에 들어온 좋아요 두 번은 한 건만 저장되고, 둘 다 '좋아요한 상태'로 응답하며 푸시는 한 번 나간다")
    void concurrentLike_savesOnce() throws Exception {
        bothSeeSameStateBeforeWriting();

        List<ContentLikeResponse> responses = runConcurrently(2);

        assertThat(responses).containsOnly(new ContentLikeResponse(contentId, true, 1L));
        assertThat(likeRows()).isEqualTo(1);
        verify(fcmPushEventPublisher, times(1)).publish(eq(author.getId()), eq(FcmPushType.CONTENTS_LIKE), anyMap());
        verify(noticeService, times(1)).saveNotice(eq(author.getId()), eq(FcmPushType.CONTENTS_LIKE), anyMap(), eq(me.getId()), any());
    }

    @Test
    @DisplayName("동시에 들어온 취소 두 번은 둘 다 '좋아요 안 한 상태'로 응답한다 (500 없음)")
    void concurrentUnlike_noError() throws Exception {
        tx.executeWithoutResult(status -> {
            ContentLike like = ContentLike.builder().content(em.getReference(Content.class, contentId)).build();
            ReflectionTestUtils.setField(like, "createdBy", em.getReference(User.class, me.getId()));
            em.persist(like);
        });
        bothSeeSameStateBeforeWriting();

        List<ContentLikeResponse> responses = runConcurrently(2);

        assertThat(responses).containsOnly(new ContentLikeResponse(contentId, false, 0L));
        assertThat(likeRows()).isZero();
    }

    // 두 요청이 모두 "지금 상태"를 읽은 뒤에야 쓰기로 넘어가게 맞춘다 → 경합을 매번 재현
    private void bothSeeSameStateBeforeWriting() {
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger reads = new AtomicInteger();
        // 리포지토리는 인터페이스 프록시라 callRealMethod 를 못 쓴다 → 실제 빈으로 넘기는 기본 Answer 에 위임
        Answer<?> real = mockingDetails(contentLikeRepository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            Object result = real.answer(invocation);
            if (reads.getAndIncrement() < 2) {   // 두 요청의 첫 조회만 맞춘다 (충돌 뒤 재조회는 그냥 통과)
                barrier.await(5, TimeUnit.SECONDS);
            }
            return result;
        }).when(contentLikeRepository).existsByContentIdAndCreatedById(any(), any());
    }

    private List<ContentLikeResponse> runConcurrently(int requests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<ContentLikeResponse>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(pool.submit(this::likeAsMe));
            }
            List<ContentLikeResponse> responses = new ArrayList<>();
            for (Future<ContentLikeResponse> future : futures) {
                responses.add(future.get(20, TimeUnit.SECONDS));   // 요청이 예외로 끝나면 여기서 실패
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }

    // 서비스 쪽 감사(@CreatedBy)가 로그인 유저로 채워지도록 요청 스레드에 인증을 둔다
    private ContentLikeResponse likeAsMe() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(me.getId(), null, List.of()));
        try {
            return contentService.like(contentId);
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

    private long likeRows() {
        return tx.execute(status -> em.createQuery(
                        "select count(cl) from ContentLike cl where cl.content.id = :id", Long.class)
                .setParameter("id", contentId)
                .getSingleResult());
    }
}
