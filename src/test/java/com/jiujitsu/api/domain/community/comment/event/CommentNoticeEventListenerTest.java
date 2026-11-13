package com.jiujitsu.api.domain.community.comment.event;

import com.jiujitsu.api.domain.community.board.repository.BoardHideRepository;
import com.jiujitsu.api.domain.community.report.repository.ReportRepository;
import com.jiujitsu.api.domain.notice.service.NoticePreview;
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
import static org.mockito.Mockito.verifyNoInteractions;

// 댓글 알림 푸시 (#196): 제목은 알림 종류 문구, 본문은 알림함 미리보기(NoticePreview) — 거른 뒤에만 조회
@ExtendWith(MockitoExtension.class)
class CommentNoticeEventListenerTest {

    @InjectMocks private CommentNoticeEventListener listener;

    @Mock private NoticeService noticeService;
    @Mock private NoticePreview noticePreview;
    @Mock private FcmPushEventPublisher fcmPushEventPublisher;
    @Mock private UserBlockRepository userBlockRepository;
    @Mock private BoardHideRepository boardHideRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private com.jiujitsu.api.domain.user.repository.UserRepository userRepository;

    private final Map<String, String> data = Map.of("type", "BOARD_DETAIL", "data", "10");

    @Test
    @DisplayName("푸시 본문에 알림함 미리보기 값(게시글 제목)을 실어 보낸다")
    void publishesPreviewAsPushBody() {
        CommentNoticeEvent event = new CommentNoticeEvent(2L, 10L, FcmPushType.NEW_COMMENTS, data, 1L, 100L);
        given(noticeService.isContentNoticeEnabled(2L, 10L)).willReturn(true);
        given(noticePreview.of(FcmPushType.NEW_COMMENTS, 10L, 100L)).willReturn("오늘 오픈매트 후기");

        listener.handle(event);

        verify(fcmPushEventPublisher).publish(2L, FcmPushType.NEW_COMMENTS, data,
                "내 게시글에 새로운 댓글이 달렸어요.", "오늘 오픈매트 후기");
        verify(noticeService).saveNotice(2L, FcmPushType.NEW_COMMENTS, data, 1L, 100L);
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "{0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"차단", "숨김", "신고"})
    @DisplayName("보낸 사람을 차단했거나 글을 숨기거나 신고했으면 푸시·저장 없이, 본문도 조회하지 않는다")
    void filtered_skipsPreviewLookup(String reason) {
        CommentNoticeEvent event = new CommentNoticeEvent(2L, 10L, FcmPushType.NEW_COMMENTS, data, 1L, 100L);
        given(noticeService.isContentNoticeEnabled(2L, 10L)).willReturn(true);
        switch (reason) {
            case "차단" -> given(userBlockRepository.existsByBlockerIdAndBlockedId(2L, 1L)).willReturn(true);
            case "숨김" -> given(boardHideRepository.findByCreatedByIdAndContentId(2L, 10L))
                    .willReturn(java.util.Optional.of(org.mockito.Mockito.mock(com.jiujitsu.api.domain.community.board.entity.BoardHide.class)));
            default -> given(reportRepository.existsByCreatedByIdAndReportTypeAndTargetId(
                    2L, com.jiujitsu.api.domain.community.report.entity.ReportType.BOARD, 10L)).willReturn(true);
        }

        listener.handle(event);

        verifyNoInteractions(noticePreview, fcmPushEventPublisher);
        verify(noticeService, org.mockito.Mockito.never()).saveNotice(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("게시물 알림을 껐으면 푸시도 없고 본문도 조회하지 않는다")
    void noticeOff_skipsPreviewLookup() {
        CommentNoticeEvent event = new CommentNoticeEvent(2L, 10L, FcmPushType.NEW_COMMENTS, data, 1L, 100L);
        given(noticeService.isContentNoticeEnabled(2L, 10L)).willReturn(false);

        listener.handle(event);

        verifyNoInteractions(noticePreview, fcmPushEventPublisher);
    }

    @Test
    @DisplayName("받는 사람이 탈퇴했으면 푸시·저장 없이, 본문도 조회하지 않는다 (#216)")
    void withdrawnRecipient_skipped() {
        CommentNoticeEvent event = new CommentNoticeEvent(2L, 10L, FcmPushType.NEW_CHILD_COMMENTS, data, 1L, 100L);
        given(noticeService.isContentNoticeEnabled(2L, 10L)).willReturn(true);
        given(userRepository.findByIdAndStatus(2L, com.jiujitsu.api.domain.user.entity.UserStatus.DELETED))
                .willReturn(java.util.Optional.of(org.mockito.Mockito.mock(com.jiujitsu.api.domain.user.entity.User.class)));

        listener.handle(event);

        verifyNoInteractions(noticePreview, fcmPushEventPublisher);
    }
}

