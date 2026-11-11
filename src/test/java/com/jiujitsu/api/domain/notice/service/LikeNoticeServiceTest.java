package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardHide;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.board.repository.BoardHideRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.content.repository.ContentLikeRepository;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.repository.ReportRepository;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.event.LikeNoticeEvent;
import com.jiujitsu.api.domain.notice.factory.NoticeFactory;
import com.jiujitsu.api.domain.notice.repository.NoticeRepository;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import com.jiujitsu.api.global.properties.LikePushProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 좋아요 알림 (#196): 기준(1·5·10, 이후 10마다)에 처음 닿을 때만 푸시, 카드는 한 장
 */
class LikeNoticeServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-11T09:00:00Z"), SEOUL);
    private static final Long OWNER = 2L;
    private static final Long CONTENT = 10L;
    private static final Long COMMENT = 100L;

    private final NoticeRepository noticeRepository = mock(NoticeRepository.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final BoardHideRepository boardHideRepository = mock(BoardHideRepository.class);
    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final ContentLikeRepository contentLikeRepository = mock(ContentLikeRepository.class);
    private final CommentLikeRepository commentLikeRepository = mock(CommentLikeRepository.class);
    private final FcmPushEventPublisher fcmPushEventPublisher = mock(FcmPushEventPublisher.class);
    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final CommunityCommentsRepository communityCommentsRepository = mock(CommunityCommentsRepository.class);
    private final CommunityComments comment = mock(CommunityComments.class);

    private final LikeNoticeService service = new LikeNoticeService(
            noticeRepository, new NoticeFactory(), noticeService, boardHideRepository, reportRepository,
            contentLikeRepository, commentLikeRepository, new NoticePreview(boardRepository, communityCommentsRepository), fcmPushEventPublisher,
            new LikePushProperties(List.of(1, 5, 10), 10), CLOCK);

    private Notice card;

    @BeforeEach
    void setUp() {
        lenient().when(noticeService.isContentNoticeEnabled(OWNER, CONTENT)).thenReturn(true);
        // 푸시 본문: 게시글 제목 / 좋아요 받은 댓글
        Board board = mock(Board.class);
        lenient().when(board.getTitle()).thenReturn("오늘 오픈매트 후기");
        lenient().when(boardRepository.findByContent_Id(CONTENT)).thenReturn(Optional.of(board));
        lenient().when(comment.getBody()).thenReturn("저도 그 기술 배워보고 싶어요");
        lenient().when(communityCommentsRepository.findById(COMMENT)).thenReturn(Optional.of(comment));
        lenient().when(boardHideRepository.findByCreatedByIdAndContentId(OWNER, CONTENT)).thenReturn(Optional.empty());
        lenient().when(reportRepository.existsByCreatedByIdAndReportTypeAndTargetId(OWNER, ReportType.BOARD, CONTENT))
                .thenReturn(false);
        // 카드가 없으면 만들고, 그 뒤로는 같은 카드를 잠가 읽는다
        lenient().when(noticeRepository.findForUpdateByUserIdAndGroupKey(eq(OWNER), anyString()))
                .thenAnswer(inv -> Optional.ofNullable(card));
        lenient().when(noticeRepository.saveAndFlush(any(Notice.class))).thenAnswer(inv -> {
            card = inv.getArgument(0);
            return card;
        });
    }

    private static LikeNoticeEvent contentLike() {
        return new LikeNoticeEvent(OWNER, CONTENT, null, FcmPushType.CONTENTS_LIKE,
                Map.of("type", "BOARD_DETAIL", "data", CONTENT.toString()));
    }

    private static LikeNoticeEvent commentLike() {
        return new LikeNoticeEvent(OWNER, CONTENT, COMMENT, FcmPushType.COMMENTS_LIKE,
                Map.of("type", "BOARD_DETAIL", "data", CONTENT.toString()));
    }

    private void contentLikes(long count) {
        given(contentLikeRepository.countContentLikeByContentIds(List.of(CONTENT)))
                .willReturn(count == 0 ? List.of() : List.<Object[]>of(new Object[]{CONTENT, count}));
    }

    private void commentLikes(long count) {
        given(commentLikeRepository.countGroupByCommentIds(List.of(COMMENT)))
                .willReturn(List.<Object[]>of(new Object[]{COMMENT, count}));
    }

    @Test
    @DisplayName("첫 좋아요: 카드를 만들고 \"좋아요가 1개\" 푸시, 안 읽음·알림 시각 지금")
    void first_pushesAndCreatesCard() {
        contentLikes(1);

        assertThat(service.handle(contentLike())).isTrue();

        verify(fcmPushEventPublisher).publish(eq(OWNER), eq(FcmPushType.CONTENTS_LIKE),
                eq(Map.of("type", "BOARD_DETAIL", "data", "10")), eq("내 게시글에 좋아요가 1개 달렸어요"), eq("오늘 오픈매트 후기"));
        assertThat(card.getGroupKey()).isEqualTo("CONTENTS_LIKE:10");
        assertThat(card.getLikePushedCount()).isEqualTo(1);
        assertThat(card.getIsRead()).isFalse();
        assertThat(card.getNotifiedAt()).isEqualTo(LocalDateTime.of(2026, 11, 11, 18, 0));
        assertThat(card.getSenderUserId()).isNull();
    }

    @Test
    @DisplayName("2~4개는 푸시 없음, 카드 그대로 (읽음 상태·시각 유지)")
    void betweenThresholds_noPush() {
        contentLikes(1);
        service.handle(contentLike());
        card.setRead();
        LocalDateTime pushedAt = card.getNotifiedAt();

        for (long n = 2; n <= 4; n++) {
            contentLikes(n);
            assertThat(service.handle(contentLike())).isFalse();
        }

        verify(fcmPushEventPublisher).publish(anyLong(), any(), anyMap(), anyString(), any()); // 1개 때 한 번뿐
        assertThat(card.getIsRead()).isTrue();
        assertThat(card.getNotifiedAt()).isEqualTo(pushedAt);
        assertThat(card.getLikePushedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("5개: 다시 안 읽음으로 올리고 \"5개\" 푸시")
    void fifth_pushesAgain() {
        contentLikes(1);
        service.handle(contentLike());
        card.setRead();

        contentLikes(5);
        assertThat(service.handle(contentLike())).isTrue();

        verify(fcmPushEventPublisher).publish(eq(OWNER), any(), anyMap(), eq("내 게시글에 좋아요가 5개 달렸어요"), eq("오늘 오픈매트 후기"));
        assertThat(card.getIsRead()).isFalse();
        assertThat(card.getLikePushedCount()).isEqualTo(5);
        assertThat(card.getTitle()).isEqualTo("내 게시글에 좋아요가 5개 달렸어요");
    }

    @Test
    @DisplayName("취소 후 다시 눌러 5 → 4 → 5 가 돼도 5개 푸시는 한 번")
    void cancelAndRelike_noDuplicatePush() {
        contentLikes(5);
        assertThat(service.handle(contentLike())).isTrue();

        // 4개로 줄었다가(취소는 이벤트 없음) 다시 5개
        contentLikes(5);
        assertThat(service.handle(contentLike())).isFalse();

        verify(fcmPushEventPublisher).publish(anyLong(), any(), anyMap(), anyString(), any());
    }

    @Test
    @DisplayName("10개 뒤로는 20, 30 … 에서만")
    void afterTen_everyTen() {
        contentLikes(10);
        service.handle(contentLike());
        contentLikes(19);
        assertThat(service.handle(contentLike())).isFalse();
        contentLikes(20);
        assertThat(service.handle(contentLike())).isTrue();
        assertThat(card.getLikePushedCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("댓글 좋아요도 같은 기준, 카드는 댓글마다 (refId = 댓글)")
    void commentLike_sameRule() {
        commentLikes(1);
        assertThat(service.handle(commentLike())).isTrue();
        assertThat(card.getGroupKey()).isEqualTo("COMMENTS_LIKE:100");
        assertThat(card.getRefId()).isEqualTo(COMMENT);
        assertThat(card.getData()).isEqualTo("10");

        commentLikes(3);
        assertThat(service.handle(commentLike())).isFalse();
        commentLikes(5);
        assertThat(service.handle(commentLike())).isTrue();

        verify(fcmPushEventPublisher).publish(eq(OWNER), eq(FcmPushType.COMMENTS_LIKE), anyMap(),
                eq("내 댓글에 좋아요가 5개 달렸어요"), eq("저도 그 기술 배워보고 싶어요"));
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "댓글 {0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"삭제", "숨김"})
    @DisplayName("좋아요 받은 댓글이 그사이 삭제·숨김됐으면 푸시 본문은 비운다 (알림함 미리보기와 같음)")
    void commentLike_deletedOrHiddenComment_noBody(String state) {
        if (state.equals("삭제")) {
            given(comment.isDeleted()).willReturn(true);
        } else {
            given(comment.isHidden()).willReturn(true);
        }
        commentLikes(1);

        assertThat(service.handle(commentLike())).isTrue();

        verify(fcmPushEventPublisher).publish(eq(OWNER), eq(FcmPushType.COMMENTS_LIKE), anyMap(),
                eq("내 댓글에 좋아요가 1개 달렸어요"), isNull());
    }

    @Test
    @DisplayName("이벤트가 늦게 처리돼 좋아요가 이미 0개면 카드만 있고 푸시 없음")
    void zeroLikes_noPush() {
        contentLikes(0);
        assertThat(service.handle(contentLike())).isFalse();
        verify(fcmPushEventPublisher, never()).publish(anyLong(), any(), anyMap(), anyString(), any());
    }

    @Test
    @DisplayName("게시물 알림을 껐으면 카드·푸시 없음")
    void contentNoticeOff() {
        given(noticeService.isContentNoticeEnabled(OWNER, CONTENT)).willReturn(false);
        assertThat(service.handle(contentLike())).isFalse();
        verify(noticeRepository, never()).saveAndFlush(any());
        verify(fcmPushEventPublisher, never()).publish(anyLong(), any(), anyMap(), anyString(), any());
    }

    @Test
    @DisplayName("숨긴 글·신고한 글이면 카드·푸시 없음")
    void hiddenOrReported() {
        given(boardHideRepository.findByCreatedByIdAndContentId(OWNER, CONTENT)).willReturn(Optional.of(mock(BoardHide.class)));
        assertThat(service.handle(contentLike())).isFalse();

        given(boardHideRepository.findByCreatedByIdAndContentId(OWNER, CONTENT)).willReturn(Optional.empty());
        given(reportRepository.existsByCreatedByIdAndReportTypeAndTargetId(OWNER, ReportType.BOARD, CONTENT)).willReturn(true);
        assertThat(service.handle(contentLike())).isFalse();

        verify(noticeRepository, never()).saveAndFlush(any());
        verify(contentLikeRepository, never()).countContentLikeByContentIds(anyList());
    }
}
