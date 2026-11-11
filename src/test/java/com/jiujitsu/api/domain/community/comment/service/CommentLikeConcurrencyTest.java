package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommentLike;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.factory.CommentFactory;
import com.jiujitsu.api.domain.community.comment.factory.CommentLikeFactory;
import com.jiujitsu.api.domain.community.comment.mapper.CommentLikeMapper;
import com.jiujitsu.api.domain.community.comment.mapper.CommentMapper;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.notice.event.LikeNoticeEvent;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.security.AuditorAwareImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

/**
 * 같은 사람의 댓글 좋아요가 동시에 들어와도 한 건만 저장되고 500 이 나지 않는지 실제 트랜잭션으로 검증한다. (#199)
 * 요청마다 트랜잭션이 따로 커밋돼야 하므로 테스트 트랜잭션을 끄고, 끝나면 컨텍스트(H2)를 버린다.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import({CommunityCommentsService.class, CommentLikeFactory.class, CommentLikeMapper.class,
        AuditorAwareImpl.class, CommentLikeConcurrencyTest.NoticeEventCounter.class})
class CommentLikeConcurrencyTest {

    @Autowired private CommunityCommentsService communityCommentsService;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManager em;
    @Autowired private NoticeEventCounter noticeEvents;

    @SpyBean private CommentLikeRepository commentLikeRepository;

    @MockBean private AuthenticationFacade authenticationFacade;
    @MockBean private UserBlockService userBlockService;
    @MockBean private ReportService reportService;
    @MockBean private CommentFactory commentFactory;
    @MockBean private CommentMapper commentMapper;
    @MockBean private CommunityProfileService communityProfileService;

    private User author;
    private User me;
    private Long commentId;

    @BeforeEach
    void setUp() {
        noticeEvents.events.clear();
        tx.executeWithoutResult(status -> {
            author = saveUser();
            me = saveUser();
            Content content = Content.builder().contentType(ContentType.BOARD).build();
            em.persist(content);
            CommunityComments comment = CommunityComments.builder().content(content).body("댓글").build();
            ReflectionTestUtils.setField(comment, "createdBy", author);
            em.persist(comment);
            commentId = comment.getId();
        });
        given(authenticationFacade.getCurrentUser()).willReturn(me);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("같은 사람이 같은 댓글에 좋아요를 두 번 저장하면 유니크 제약에 걸린다")
    void uniqueConstraint() {
        tx.executeWithoutResult(status -> insertLike(me));

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> insertLike(me)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(likeRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 → 취소 → 좋아요 토글과 개수, 좋아요가 저장될 때마다 알림 이벤트 (푸시 여부는 LikeNoticeService 가 개수 기준으로 거름)")
    void toggle() {
        User other = tx.execute(status -> saveUser());
        tx.executeWithoutResult(status -> insertLike(other));

        assertThat(like()).isEqualTo(new CommentLikeResponse(commentId, true, 2L));
        assertThat(like()).isEqualTo(new CommentLikeResponse(commentId, false, 1L));
        assertThat(like()).isEqualTo(new CommentLikeResponse(commentId, true, 2L));
        assertThat(noticeEvents.events).hasSize(2);
    }

    @Test
    @DisplayName("동시에 들어온 좋아요 두 번은 한 건만 저장되고, 둘 다 '좋아요한 상태'로 응답하며 알림 이벤트는 한 번 나간다")
    void concurrentLike_savesOnce() throws Exception {
        bothSeeSameStateBeforeWriting();

        List<CommentLikeResponse> responses = runConcurrently(2);

        assertThat(responses).containsOnly(new CommentLikeResponse(commentId, true, 1L));
        assertThat(likeRows()).isEqualTo(1);
        assertThat(noticeEvents.events).singleElement().satisfies(event -> {
            assertThat(event.recipientUserId()).isEqualTo(author.getId());
            assertThat(event.commentId()).isEqualTo(commentId);
            assertThat(event.pushType()).isEqualTo(FcmPushType.COMMENTS_LIKE);
        });
    }

    @Test
    @DisplayName("동시에 들어온 취소 두 번은 둘 다 '좋아요 안 한 상태'로 응답한다 (500 없음)")
    void concurrentUnlike_noError() throws Exception {
        tx.executeWithoutResult(status -> insertLike(me));
        bothSeeSameStateBeforeWriting();

        List<CommentLikeResponse> responses = runConcurrently(2);

        assertThat(responses).containsOnly(new CommentLikeResponse(commentId, false, 0L));
        assertThat(likeRows()).isZero();
    }

    @Test
    @DisplayName("제약이 생기기 전 중복 저장된 좋아요가 있어도 누르면 취소되고, 다시 누르면 한 건으로 좋아요된다")
    void existingDuplicates_unlikeAll() {
        dropUniqueConstraint();
        try {
            tx.executeWithoutResult(status -> {
                insertLike(me);
                insertLike(me);
            });
            assertThat(likeRows()).isEqualTo(2);

            assertThat(like()).isEqualTo(new CommentLikeResponse(commentId, false, 0L));
            assertThat(likeRows()).isZero();

            assertThat(like()).isEqualTo(new CommentLikeResponse(commentId, true, 1L));
            assertThat(likeRows()).isEqualTo(1);
        } finally {
            tx.executeWithoutResult(status -> em.createQuery("delete from CommentLike").executeUpdate());
            addUniqueConstraint();
        }
    }

    private CommentLikeResponse like() {
        return asMe(() -> communityCommentsService.createCommentLike(new CommentLikeRequest(commentId)));
    }

    // 두 요청이 모두 "지금 상태"를 읽은 뒤에야 쓰기로 넘어가게 맞춘다 → 경합을 매번 재현
    private void bothSeeSameStateBeforeWriting() {
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger reads = new AtomicInteger();
        // 리포지토리는 인터페이스 프록시라 callRealMethod 를 못 쓴다 → 실제 빈으로 넘기는 기본 Answer 에 위임
        Answer<?> real = mockingDetails(commentLikeRepository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            Object result = real.answer(invocation);
            if (reads.getAndIncrement() < 2) {   // 두 요청의 첫 조회만 맞춘다 (충돌 뒤 재조회는 그냥 통과)
                barrier.await(5, TimeUnit.SECONDS);
            }
            return result;
        }).when(commentLikeRepository).existsByCommentIdAndCreatedById(any(), any());
    }

    private List<CommentLikeResponse> runConcurrently(int requests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<CommentLikeResponse>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(pool.submit(() -> like()));
            }
            List<CommentLikeResponse> responses = new ArrayList<>();
            for (Future<CommentLikeResponse> future : futures) {
                responses.add(future.get(20, TimeUnit.SECONDS));   // 요청이 예외로 끝나면 여기서 실패
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }

    // 서비스 쪽 감사(@CreatedBy)가 로그인 유저로 채워지도록 요청 스레드에 인증을 둔다
    private <T> T asMe(Callable<T> call) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(me.getId(), null, List.of()));
        try {
            return call.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
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

    private void insertLike(User user) {
        CommentLike like = CommentLike.builder().comment(em.getReference(CommunityComments.class, commentId)).build();
        ReflectionTestUtils.setField(like, "createdBy", em.getReference(User.class, user.getId()));
        commentLikeRepository.saveAndFlush(like);
    }

    private long likeRows() {
        return tx.execute(status -> em.createQuery(
                        "select count(cl) from CommentLike cl where cl.comment.id = :id", Long.class)
                .setParameter("id", commentId)
                .getSingleResult());
    }

    private void dropUniqueConstraint() {
        tx.executeWithoutResult(status -> em.createNativeQuery(
                "alter table comment_like drop constraint " + CommentLike.UNIQUE_CONSTRAINT).executeUpdate());
    }

    private void addUniqueConstraint() {
        tx.executeWithoutResult(status -> em.createNativeQuery(
                "alter table comment_like add constraint " + CommentLike.UNIQUE_CONSTRAINT + " unique (comment_id, created_by)")
                .executeUpdate());
    }

    // 커밋된 요청이 보낸 좋아요 알림 이벤트만 센다 (실제 리스너 LikeNoticeEventListener 와 같은 AFTER_COMMIT)
    @TestConfiguration
    static class NoticeEventCounter {
        final List<LikeNoticeEvent> events = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        void on(LikeNoticeEvent event) {
            events.add(event);
        }
    }
}
