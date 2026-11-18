package com.jiujitsu.api.domain.notice.mapper;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 알림 목록의 commentId — 댓글 · 대댓글 · 댓글 좋아요 알림만 refId 를 그대로 (#271)
class NoticeMapperCommentIdTest {

    @Test
    @DisplayName("댓글 · 대댓글 · 댓글 좋아요 알림은 commentId = refId")
    void commentNotices() {
        for (FcmPushType type : new FcmPushType[]{FcmPushType.NEW_COMMENTS, FcmPushType.NEW_CHILD_COMMENTS, FcmPushType.COMMENTS_LIKE}) {
            assertThat(NoticeMapper.toNoticeListResponse(notice(type, 55L), "제목", null, "본문").commentId())
                    .as(type.name()).isEqualTo(55L);
        }
    }

    @Test
    @DisplayName("그 밖의 알림(글 좋아요 · 대회)은 commentId 없음")
    void otherNotices() {
        assertThat(NoticeMapper.toNoticeListResponse(notice(FcmPushType.CONTENTS_LIKE, 10L), "제목", null, "본문").commentId()).isNull();
        assertThat(NoticeMapper.toNoticeListResponse(notice(FcmPushType.COMPETITION_REGISTRATION_DEADLINE, null), "제목", null, "본문").commentId()).isNull();
    }

    private static Notice notice(FcmPushType type, Long refId) {
        return Notice.builder().id(1L).userId(2L).pushType(type).data("10").refId(refId).isRead(false).build();
    }
}
