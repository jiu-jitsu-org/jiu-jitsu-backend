package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.event.LikeNoticeEvent;
import com.jiujitsu.api.domain.notice.event.LikeNoticeEventListener;
import com.jiujitsu.api.domain.notice.factory.NoticeFactory;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 좋아요 카드 (#196): (받는 사람, 카드 키) 하나, 알림 시각 정렬, 알림 시각 기준 정리
@DataJpaTest
class NoticeLikeCardRepositoryTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 11, 18, 0);

    @Autowired private NoticeRepository noticeRepository;
    @Autowired private TestEntityManager em;

    private final NoticeFactory noticeFactory = new NoticeFactory();

    private static LikeNoticeEvent like(long recipient, long contentId) {
        return new LikeNoticeEvent(recipient, contentId, null, FcmPushType.CONTENTS_LIKE,
                Map.of("type", "BOARD_DETAIL", "data", String.valueOf(contentId)));
    }

    private Notice comment(long userId, LocalDateTime notifiedAt) {
        return em.persist(Notice.builder().userId(userId).title("댓글")
                .pushType(FcmPushType.NEW_COMMENTS).pushActionType(PushActionType.BOARD_DETAIL)
                .data("1").isRead(false).notifiedAt(notifiedAt).build());
    }

    @Test
    @DisplayName("같은 사람·같은 대상 카드는 두 장 만들 수 없다 — 위반은 카드 중복으로 알아본다")
    void sameCard_unique() {
        noticeRepository.saveAndFlush(noticeFactory.createLikeCard(like(1L, 10L)));

        assertThatThrownBy(() -> noticeRepository.saveAndFlush(noticeFactory.createLikeCard(like(1L, 10L))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(e -> assertThat(LikeNoticeEventListener.isCardDuplicate((RuntimeException) e)).isTrue());
    }

    @Test
    @DisplayName("다른 사람·다른 대상, 카드 키 없는 일반 알림은 여러 장 가능")
    void otherCards_allowed() {
        noticeRepository.saveAndFlush(noticeFactory.createLikeCard(like(1L, 10L)));
        noticeRepository.saveAndFlush(noticeFactory.createLikeCard(like(2L, 10L)));
        noticeRepository.saveAndFlush(noticeFactory.createLikeCard(like(1L, 11L)));
        comment(1L, T);
        comment(1L, T);

        assertThat(noticeRepository.findForUpdateByUserIdAndGroupKey(1L, "CONTENTS_LIKE:10")).isPresent();
        assertThat(noticeRepository.findByUserId(1L)).hasSize(4);
    }

    @Test
    @DisplayName("만들 때 알림 시각이 비어 있으면 채운다 (일반 알림)")
    void notifiedAt_filledOnPersist() {
        Notice notice = em.persist(Notice.builder().userId(1L).title("댓글").pushType(FcmPushType.NEW_COMMENTS).isRead(false).build());
        assertThat(notice.getNotifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("알림함 순서는 알림 시각 최신순 — 푸시가 간 좋아요 카드가 맨 위로")
    void order_byNotifiedAt() {
        Notice card = noticeRepository.saveAndFlush(noticeFactory.createLikeCard(like(1L, 10L)));
        card.pushLike(1, "내 게시글에 좋아요가 1개 달렸어요", T.minusHours(3));
        Notice older = comment(1L, T.minusHours(2));
        Notice newer = comment(1L, T.minusHours(1));
        em.flush();
        assertThat(noticeRepository.findByUserIdOrderByNotifiedAtDescIdDesc(1L))
                .extracting(Notice::getId).containsExactly(newer.getId(), older.getId(), card.getId());

        // 5개 도달 → 카드가 맨 위
        card.pushLike(5, "내 게시글에 좋아요가 5개 달렸어요", T);
        em.flush();
        assertThat(noticeRepository.findByUserIdOrderByNotifiedAtDescIdDesc(1L))
                .extracting(Notice::getId).containsExactly(card.getId(), newer.getId(), older.getId());
    }

    @Test
    @DisplayName("30일 정리는 알림 시각 기준 — 오래된 일반 알림만 지운다")
    void cleanup_byNotifiedAt() {
        Notice old = comment(1L, T.minusDays(31));
        Notice recent = comment(1L, T.minusDays(29));
        em.flush();

        noticeRepository.deleteExpiredExceptLikeCards(T.minusDays(30));
        em.clear();

        assertThat(noticeRepository.findById(old.getId())).isEmpty();
        assertThat(noticeRepository.findById(recent.getId())).isPresent();
    }

    @Test
    @DisplayName("읽음 처리는 is_read 만 쓴다 — 그 사이 다른 트랜잭션이 올린 기준(like_pushed_count)·제목·시각을 옛 값으로 덮지 않는다")
    void setRead_doesNotOverwriteConcurrentPush() {
        Notice card = noticeRepository.saveAndFlush(noticeFactory.createLikeCard(like(1L, 10L)));
        card.pushLike(5, "내 게시글에 좋아요가 5개 달렸어요", T.minusHours(1));
        em.flush();

        // 읽음 처리 트랜잭션이 카드를 읽어 둔 사이, 좋아요 처리(다른 트랜잭션)가 10개 기준을 커밋한 상황
        em.getEntityManager().createQuery(
                        "UPDATE Notice n SET n.likePushedCount = 10, n.title = '내 게시글에 좋아요가 10개 달렸어요', n.notifiedAt = :t WHERE n.id = :id")
                .setParameter("t", T).setParameter("id", card.getId())
                .executeUpdate();
        card.setRead();
        em.flush();
        em.clear();

        Notice saved = noticeRepository.findById(card.getId()).orElseThrow();
        assertThat(saved.getLikePushedCount()).isEqualTo(10);
        assertThat(saved.getTitle()).isEqualTo("내 게시글에 좋아요가 10개 달렸어요");
        assertThat(saved.getNotifiedAt()).isEqualTo(T);
        assertThat(saved.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("30일 정리: 푸시가 한 번도 안 간 좋아요 카드는 지운다 (잃을 기준이 없음)")
    void cleanup_deletesNeverPushedLikeCards() {
        Notice neverPushed = noticeRepository.saveAndFlush(noticeFactory.createLikeCard(like(1L, 10L)));
        em.getEntityManager().createQuery("UPDATE Notice n SET n.notifiedAt = :t WHERE n.id = :id")
                .setParameter("t", T.minusDays(31)).setParameter("id", neverPushed.getId())
                .executeUpdate();
        em.clear();

        noticeRepository.deleteExpiredExceptLikeCards(T.minusDays(30));
        em.clear();

        assertThat(noticeRepository.findById(neverPushed.getId())).isEmpty();
    }

    @Test
    @DisplayName("30일 정리는 좋아요 카드를 지우지 않는다 — 이미 보낸 기준(likePushedCount)을 잃으면 같은 기준으로 다시 푸시하게 됨")
    void cleanup_keepsLikeCards() {
        Notice card = noticeRepository.saveAndFlush(noticeFactory.createLikeCard(like(1L, 10L)));
        card.pushLike(10, "내 게시글에 좋아요가 10개 달렸어요", T.minusDays(31));
        em.flush();

        noticeRepository.deleteExpiredExceptLikeCards(T.minusDays(30));
        em.clear();

        assertThat(noticeRepository.findForUpdateByUserIdAndGroupKey(1L, "CONTENTS_LIKE:10"))
                .get().extracting(Notice::getLikePushedCount).isEqualTo(10);
    }
}
