package com.jiujitsu.api.domain.community.comment.event;

import com.jiujitsu.api.domain.community.board.repository.BoardHideRepository;
import com.jiujitsu.api.domain.community.report.repository.ReportRepository;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.repository.UserBlockRepository;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

// 댓글 알림 푸시 (#196): 제목은 알림 종류 문구, 본문은 이벤트가 담아 온 게시글 제목·내 댓글
@ExtendWith(MockitoExtension.class)
class CommentNoticeEventListenerTest {

    @InjectMocks private CommentNoticeEventListener listener;

    @Mock private NoticeService noticeService;
    @Mock private FcmPushEventPublisher fcmPushEventPublisher;
    @Mock private UserBlockRepository userBlockRepository;
    @Mock private BoardHideRepository boardHideRepository;
    @Mock private ReportRepository reportRepository;

    @Test
    @DisplayName("푸시 본문에 게시글 제목을 실어 보낸다")
    void publishesPushBody() {
        Map<String, String> data = Map.of("type", "BOARD_DETAIL", "data", "10");
        CommentNoticeEvent event = new CommentNoticeEvent(2L, 10L, FcmPushType.NEW_COMMENTS, data, 1L, 100L, "오늘 오픈매트 후기");
        given(noticeService.isContentNoticeEnabled(2L, 10L)).willReturn(true);

        listener.handle(event);

        verify(fcmPushEventPublisher).publish(2L, FcmPushType.NEW_COMMENTS, data,
                "내 게시글에 새로운 댓글이 달렸어요.", "오늘 오픈매트 후기");
        verify(noticeService).saveNotice(2L, FcmPushType.NEW_COMMENTS, data, 1L, 100L);
    }
}
