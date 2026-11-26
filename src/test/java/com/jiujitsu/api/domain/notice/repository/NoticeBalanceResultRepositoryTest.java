package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 밸런스 게임 결과 알림의 읽음 · 안 읽은 수 · 정리 (#291)
@DataJpaTest
class NoticeBalanceResultRepositoryTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 26, 11, 55);

    @Autowired private NoticeRepository noticeRepository;
    @Autowired private TestEntityManager em;

    private Long notice(long userId, FcmPushType type, String data, LocalDateTime notifiedAt) {
        return em.persist(Notice.builder().userId(userId).title("제목").body("본문")
                .pushType(type).pushActionType(PushActionType.BALANCE_DETAIL)
                .data(data).isRead(false).notifiedAt(notifiedAt).build()).getId();
    }

    private boolean isRead(Long id) {
        return noticeRepository.findById(id).orElseThrow().getIsRead();
    }

    @Test
    @DisplayName("결과를 보면 그 판의 내 결과 알림만 읽음 — 같은 판 댓글 알림 · 다른 판 · 다른 사람은 그대로")
    void marksOnlyResultNoticeOfThatGame() {
        Long result = notice(1L, FcmPushType.BALANCE_RESULT, "100", T);
        Long reply = notice(1L, FcmPushType.NEW_CHILD_COMMENTS, "100", T);
        Long otherGame = notice(1L, FcmPushType.BALANCE_RESULT, "101", T);
        Long otherUser = notice(2L, FcmPushType.BALANCE_RESULT, "100", T);
        em.flush();
        em.clear();

        assertThat(noticeRepository.markReadByUserIdAndContentAndPushType(1L, "100", FcmPushType.BALANCE_RESULT)).isEqualTo(1);
        assertThat(noticeRepository.markReadByUserIdAndContentAndPushType(1L, "100", FcmPushType.BALANCE_RESULT)).isZero();
        em.clear();

        assertThat(isRead(result)).isTrue();
        assertThat(isRead(reply)).isFalse();
        assertThat(isRead(otherGame)).isFalse();
        assertThat(isRead(otherUser)).isFalse();
    }

    @Test
    @DisplayName("상세를 열면(기존 동작) 결과 알림도 함께 읽음")
    void detailMarksRead() {
        Long result = notice(1L, FcmPushType.BALANCE_RESULT, "100", T);
        em.flush();
        em.clear();

        noticeRepository.markReadByUserIdAndContent(1L, "100", List.of(PushActionType.BOARD_DETAIL, PushActionType.BALANCE_DETAIL));
        em.clear();

        assertThat(isRead(result)).isTrue();
    }

    @Test
    @DisplayName("안 읽은 수에 세고, 30일이 지나면 다른 알림처럼 지워진다")
    void unreadCountAndCleanup() {
        Long old = notice(1L, FcmPushType.BALANCE_RESULT, "100", T.minusDays(31));
        Long recent = notice(1L, FcmPushType.BALANCE_RESULT, "101", T);
        em.flush();
        em.clear();

        assertThat(noticeRepository.countUnreadFiltered(1L, List.of(-1L), List.of("-1"), PushActionType.COMPETITION_DETAIL)).isEqualTo(2);

        noticeRepository.deleteExpiredKeepingPushedLikeCards(T.minusDays(30));
        em.clear();
        assertThat(noticeRepository.findById(old)).isEmpty();
        assertThat(noticeRepository.findById(recent)).isPresent();
    }
}
