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

// 글 · 밸런스 게임 상세를 열면 그 콘텐츠에 대한 내 안 읽은 알림을 읽음으로 (#246)
@DataJpaTest
class NoticeReadOnDetailRepositoryTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 17, 18, 0);
    private static final List<PushActionType> CONTENT_ACTIONS = List.of(PushActionType.BOARD_DETAIL, PushActionType.BALANCE_DETAIL);

    @Autowired private NoticeRepository noticeRepository;
    @Autowired private TestEntityManager em;

    private Long notice(long userId, FcmPushType type, PushActionType action, String data) {
        return em.persist(Notice.builder().userId(userId).title(type.getTitle())
                .pushType(type).pushActionType(action)
                .data(data).isRead(false).notifiedAt(T).build()).getId();
    }

    private boolean isRead(Long id) {
        return noticeRepository.findById(id).orElseThrow().getIsRead();
    }

    @Test
    @DisplayName("그 글에 대한 내 댓글 · 답글 · 좋아요 알림만 읽음 — 다른 글 · 다른 사람 · 대회 알림(data 가 대회 id)은 그대로")
    void marksOnlyMyNoticesOfThatContent() {
        Long comment = notice(1L, FcmPushType.NEW_COMMENTS, PushActionType.BOARD_DETAIL, "10");
        Long reply = notice(1L, FcmPushType.NEW_CHILD_COMMENTS, PushActionType.BOARD_DETAIL, "10");
        Long like = notice(1L, FcmPushType.COMMENTS_LIKE, PushActionType.BOARD_DETAIL, "10");
        Long otherContent = notice(1L, FcmPushType.NEW_COMMENTS, PushActionType.BOARD_DETAIL, "11");
        Long otherUser = notice(2L, FcmPushType.NEW_COMMENTS, PushActionType.BOARD_DETAIL, "10");
        Long competition = notice(1L, FcmPushType.COMPETITION_EARLY_BIRD_DEADLINE, PushActionType.COMPETITION_DETAIL, "10");
        em.flush();
        em.clear();

        int updated = noticeRepository.markReadByUserIdAndContent(1L, "10", CONTENT_ACTIONS);
        em.clear();

        assertThat(updated).isEqualTo(3);
        assertThat(isRead(comment)).isTrue();
        assertThat(isRead(reply)).isTrue();
        assertThat(isRead(like)).isTrue();
        assertThat(isRead(otherContent)).isFalse();
        assertThat(isRead(otherUser)).isFalse();
        assertThat(isRead(competition)).isFalse();
    }

    @Test
    @DisplayName("밸런스 게임 알림도 같은 기준, 이미 읽은 알림은 다시 세지 않음")
    void balanceGame_and_alreadyRead() {
        Long balance = notice(1L, FcmPushType.NEW_COMMENTS, PushActionType.BALANCE_DETAIL, "20");
        em.flush();
        em.clear();

        assertThat(noticeRepository.markReadByUserIdAndContent(1L, "20", CONTENT_ACTIONS)).isEqualTo(1);
        assertThat(noticeRepository.markReadByUserIdAndContent(1L, "20", CONTENT_ACTIONS)).isZero();
        em.clear();
        assertThat(isRead(balance)).isTrue();
    }
}
