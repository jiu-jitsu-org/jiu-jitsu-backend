package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.board.service.BoardHideService;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentLikeRepository;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.notice.dto.NoticeListResponse;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.factory.NoticeFactory;
import com.jiujitsu.api.domain.notice.repository.ContentNoticeSettingRepository;
import com.jiujitsu.api.domain.notice.repository.NoticeRepository;
import com.jiujitsu.api.domain.notice.repository.UserNoticeSettingRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * 알림함의 좋아요 카드 (#196): 제목 숫자 = 지금 좋아요 수, 본문 = 게시글 제목 / 댓글, 0개면 숨김, 시각 = 마지막 푸시
 */
@ExtendWith(MockitoExtension.class)
class NoticeServiceLikeTest {

    private static final LocalDateTime PUSHED_AT = LocalDateTime.of(2026, 11, 11, 18, 0);

    @InjectMocks private NoticeService noticeService;

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticeFactory noticeFactory;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private UserNoticeSettingRepository userNoticeSettingRepository;
    @Mock private ContentNoticeSettingRepository contentNoticeSettingRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private CommunityCommentsRepository communityCommentsRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private UserBlockService userBlockService;
    @Mock private BoardHideService boardHideService;
    @Mock private ReportService reportService;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private ContentLikeRepository contentLikeRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    // 마지막 푸시(PUSHED_AT) 하루 뒤
    @Spy private Clock clock = Clock.fixed(Instant.parse("2026-11-12T09:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final User me = User.builder().id(1L).role(UserRole.USER).build();

    @BeforeEach
    void setUp() {
        lenient().when(authenticationFacade.getCurrentUser()).thenReturn(me);
        lenient().when(boardHideService.getHiddenContentIds()).thenReturn(List.of());
        lenient().when(reportService.getReportedTargetIds(ReportType.BOARD)).thenReturn(List.of());
        lenient().when(userBlockService.getBlockedUserIds()).thenReturn(List.of(99L));
    }

    private static Notice contentLikeCard(long id, long contentId, boolean read) {
        return Notice.builder().id(id).userId(1L)
                .pushType(FcmPushType.CONTENTS_LIKE).pushActionType(PushActionType.BOARD_DETAIL)
                .title("내 게시글에 좋아요가 5개 달렸어요")
                .data(String.valueOf(contentId))
                .groupKey("CONTENTS_LIKE:" + contentId).likePushedCount(5)
                .notifiedAt(PUSHED_AT).isRead(read)
                .build();
    }

    private static Notice commentLikeCard(long id, long contentId, long commentId) {
        return Notice.builder().id(id).userId(1L)
                .pushType(FcmPushType.COMMENTS_LIKE).pushActionType(PushActionType.BOARD_DETAIL)
                .title("내 댓글에 좋아요가 1개 달렸어요")
                .data(String.valueOf(contentId)).refId(commentId)
                .groupKey("COMMENTS_LIKE:" + commentId).likePushedCount(1)
                .notifiedAt(PUSHED_AT).isRead(false)
                .build();
    }

    private void stubContentExists(long contentId, String title) {
        Content content = mock(Content.class);
        given(content.getId()).willReturn(contentId);
        given(contentRepository.findAllById(anySet())).willReturn(List.of(content));
        Board board = mock(Board.class);
        given(board.getContent()).willReturn(content);
        given(board.getTitle()).willReturn(title);
        given(boardRepository.findAllByContentIdIn(anyList())).willReturn(List.of(board));
    }

    @Test
    @DisplayName("게시글 좋아요 카드: 제목은 지금 좋아요 수(기준 사이 7개), 본문 게시글 제목, 시각은 마지막 푸시")
    void contentLikeCard_titleUsesCurrentCount() {
        given(noticeRepository.findByUserIdOrderByNotifiedAtDescIdDesc(1L)).willReturn(List.of(contentLikeCard(1, 10, true)));
        given(contentLikeRepository.countContentLikeByContentIds(List.of(10L)))
                .willReturn(List.<Object[]>of(new Object[]{10L, 7L}));
        stubContentExists(10L, "오늘 오픈매트 후기");

        List<NoticeListResponse> list = noticeService.getNoticeList();

        assertThat(list).hasSize(1);
        NoticeListResponse card = list.get(0);
        assertThat(card.title()).isEqualTo("내 게시글에 좋아요가 7개 달렸어요");
        assertThat(card.body()).isEqualTo("오늘 오픈매트 후기");
        assertThat(card.createdAt()).isEqualTo(PUSHED_AT);
        assertThat(card.data()).isEqualTo("10");
        assertThat(card.deletedStatus()).isNull();
    }

    @Test
    @DisplayName("댓글 좋아요 카드: 본문은 좋아요 받은 댓글, 개수는 댓글 좋아요 수")
    void commentLikeCard_bodyIsComment() {
        given(noticeRepository.findByUserIdOrderByNotifiedAtDescIdDesc(1L)).willReturn(List.of(commentLikeCard(1, 10, 100)));
        given(commentLikeRepository.countGroupByCommentIds(List.of(100L)))
                .willReturn(List.<Object[]>of(new Object[]{100L, 3L}));
        stubContentExists(10L, "글 제목");
        CommunityComments comment = mock(CommunityComments.class);
        given(comment.getId()).willReturn(100L);
        given(comment.getBody()).willReturn("저도 그 기술 배워보고 싶어요");
        given(communityCommentsRepository.findAllById(anySet())).willReturn(List.of(comment));

        NoticeListResponse card = noticeService.getNoticeList().get(0);

        assertThat(card.title()).isEqualTo("내 댓글에 좋아요가 3개 달렸어요");
        assertThat(card.body()).isEqualTo("저도 그 기술 배워보고 싶어요");
    }

    @Test
    @DisplayName("좋아요가 전부 취소돼 0개인 카드는 목록·안 읽은 수에서 빠진다")
    void zeroLikeCard_hidden() {
        given(noticeRepository.findByUserIdOrderByNotifiedAtDescIdDesc(1L))
                .willReturn(List.of(contentLikeCard(1, 10, false), contentLikeCard(2, 11, false)));
        given(noticeRepository.findByUserIdAndIsReadFalse(1L))
                .willReturn(List.of(contentLikeCard(1, 10, false), contentLikeCard(2, 11, false)));
        given(contentLikeRepository.countContentLikeByContentIds(anyList()))
                .willReturn(List.<Object[]>of(new Object[]{11L, 1L})); // 10번 글은 0개
        stubContentExists(11L, "글");

        assertThat(noticeService.getNoticeList()).extracting(NoticeListResponse::id).containsExactly(2L);
        assertThat(noticeService.getUnreadCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("푸시가 한 번도 안 간 카드(첫 기준 전)와 마지막 푸시가 30일 지난 카드는 목록·안 읽은 수에서 빠진다")
    void beforeFirstPushOrExpired_hidden() {
        Notice beforeFirstPush = Notice.builder().id(1L).userId(1L)
                .pushType(FcmPushType.CONTENTS_LIKE).pushActionType(PushActionType.BOARD_DETAIL)
                .title("내 게시글에 좋아요가 달렸어요.").data("10")
                .groupKey("CONTENTS_LIKE:10").likePushedCount(0)
                .notifiedAt(PUSHED_AT).isRead(true).build();
        Notice expired = Notice.builder().id(2L).userId(1L)
                .pushType(FcmPushType.CONTENTS_LIKE).pushActionType(PushActionType.BOARD_DETAIL)
                .title("내 게시글에 좋아요가 10개 달렸어요").data("11")
                .groupKey("CONTENTS_LIKE:11").likePushedCount(10)
                .notifiedAt(PUSHED_AT.minusDays(30)).isRead(false).build();
        Notice live = contentLikeCard(3, 12, false);
        given(noticeRepository.findByUserIdOrderByNotifiedAtDescIdDesc(1L)).willReturn(List.of(beforeFirstPush, live, expired));
        given(noticeRepository.findByUserIdAndIsReadFalse(1L)).willReturn(List.of(live, expired));
        given(contentLikeRepository.countContentLikeByContentIds(anyList())).willReturn(List.of(
                new Object[]{10L, 3L}, new Object[]{11L, 14L}, new Object[]{12L, 5L}));
        stubContentExists(12L, "글");

        assertThat(noticeService.getNoticeList()).extracting(NoticeListResponse::id).containsExactly(3L);
        assertThat(noticeService.getUnreadCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 카드는 발신자가 없어 차단 목록에 걸리지 않는다")
    void likeCard_notFilteredByBlock() {
        given(noticeRepository.findByUserIdAndIsReadFalse(1L)).willReturn(List.of(contentLikeCard(1, 10, false)));
        given(contentLikeRepository.countContentLikeByContentIds(anyList()))
                .willReturn(List.<Object[]>of(new Object[]{10L, 1L}));

        assertThat(noticeService.getUnreadCount()).isEqualTo(1);
    }
}
