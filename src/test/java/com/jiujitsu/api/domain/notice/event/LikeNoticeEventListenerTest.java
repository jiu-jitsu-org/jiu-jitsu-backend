package com.jiujitsu.api.domain.notice.event;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.service.LikeNoticeService;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// 좋아요 알림 리스너 (#196): 카드 동시 생성은 한 번 더, 그 밖의 실패는 좋아요 응답에 영향 없이 로그만
class LikeNoticeEventListenerTest {

    private final LikeNoticeService likeNoticeService = mock(LikeNoticeService.class);
    private final LikeNoticeEventListener listener = new LikeNoticeEventListener(likeNoticeService);
    private final LikeNoticeEvent event = new LikeNoticeEvent(2L, 10L, null, FcmPushType.CONTENTS_LIKE,
            Map.of("type", "BOARD_DETAIL", "data", "10"));

    @Test
    @DisplayName("카드 유니크 충돌(동시에 처음 만듦)이면 한 번 더 처리한다")
    void duplicateCard_retries() {
        given(likeNoticeService.handle(event))
                .willThrow(new DataIntegrityViolationException("could not execute statement",
                        new RuntimeException("duplicate key value violates unique constraint \"" + Notice.GROUP_KEY_UNIQUE_CONSTRAINT + "\"")))
                .willReturn(true);

        listener.handle(event);

        verify(likeNoticeService, times(2)).handle(event);
    }

    @Test
    @DisplayName("다른 실패는 다시 하지 않고 던지지도 않는다 (좋아요는 이미 커밋됨)")
    void otherFailure_loggedOnly() {
        given(likeNoticeService.handle(event)).willThrow(new DataIntegrityViolationException("CHECK constraint notice_push_type_check"));

        assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();
        verify(likeNoticeService, times(1)).handle(event);
    }
}
