package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsWriteRequest;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.event.CommentNoticeEvent;
import com.jiujitsu.api.domain.community.comment.factory.CommentFactory;
import com.jiujitsu.api.domain.community.comment.factory.CommentLikeFactory;
import com.jiujitsu.api.domain.community.comment.mapper.CommentLikeMapper;
import com.jiujitsu.api.domain.community.comment.mapper.CommentMapper;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 대댓글의 답글 버튼으로 쓴 답글(mentionCommentId, #216)의 알림 받는 사람
 * 스레드: 최상위 댓글(100, A) ← 대댓글(200, B). 나(ME)가 같은 스레드에 답글을 쓴다. 게시글 작성자는 W
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)   // 스레드(게시글·최상위·대댓글)를 매번 같은 모양으로 스텁하고, 케이스마다 쓰는 부분만 다르다
class CommunityCommentsMentionNoticeTest {

    @InjectMocks
    private CommunityCommentsService communityCommentsService;

    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private CommunityCommentsRepository communityCommentsRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private UserBlockService userBlockService;
    @Mock private ReportService reportService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CommentFactory commentFactory;
    @Mock private CommentMapper commentMapper;
    @Mock private CommentLikeFactory commentLikeFactory;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private CommunityProfileService communityProfileService;
    @Mock private TransactionTemplate transactionTemplate;

    private static final Long CONTENT_ID = 10L;
    private static final Long TOP_ID = 100L;
    private static final Long REPLY_ID = 200L;
    private static final Long ME = 1L;
    private static final Long W = 2L;
    private static final Long A = 3L;
    private static final Long B = 4L;

    private Content content;
    private CommunityComments newComment;

    @BeforeEach
    void setUp() {
        content = mock(Content.class);
        given(content.getId()).willReturn(CONTENT_ID);
        given(content.getContentType()).willReturn(ContentType.BOARD);
        User me = user(ME);
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(contentRepository.findById(CONTENT_ID)).willReturn(Optional.of(content));
        newComment = mock(CommunityComments.class);
    }

    private void givenThread(Long boardWriterId, Long topAuthorId, CommunityComments reply) {
        User boardWriter = user(boardWriterId);
        given(content.getCreatedBy()).willReturn(boardWriter);
        CommunityComments top = comment(TOP_ID, topAuthorId, 0L);
        given(communityCommentsRepository.findById(TOP_ID)).willReturn(Optional.of(top));
        if (reply != null) {
            given(communityCommentsRepository.findById(reply.getId())).willReturn(Optional.of(reply));
        }
    }

    private void givenCreated() {
        given(commentFactory.createComments(any(), any(), any())).willReturn(newComment);
    }

    private CommunityComments comment(Long id, Long authorId, Long parentId) {
        User author = user(authorId);
        CommunityComments comment = mock(CommunityComments.class);
        given(comment.getId()).willReturn(id);
        given(comment.getCreatedBy()).willReturn(author);
        given(comment.getParentId()).willReturn(parentId);
        return comment;
    }

    private User user(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private void write(Long mentionCommentId) {
        communityCommentsService.createComment(new CommunityCommentsWriteRequest(CONTENT_ID, TOP_ID, "@B 혹시 기 입고 하시나요?", mentionCommentId));
    }

    // 받는 사람 → 알림 종류
    private Map<Long, FcmPushType> published(int expectedCount) {
        ArgumentCaptor<CommentNoticeEvent> captor = ArgumentCaptor.forClass(CommentNoticeEvent.class);
        verify(eventPublisher, times(expectedCount)).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .collect(Collectors.toMap(CommentNoticeEvent::recipientUserId, CommentNoticeEvent::pushType));
    }

    @Test
    @DisplayName("태그 답글: 답글 알림은 태그된 대댓글 작성자에게만, 최상위 댓글 작성자는 받지 않는다. 새 대댓글에 태그를 남긴다")
    void mention_notifiesMentionedOnly() {
        givenThread(W, A, comment(REPLY_ID, B, TOP_ID));
        givenCreated();

        write(REPLY_ID);

        assertThat(published(2)).containsExactlyInAnyOrderEntriesOf(Map.of(
                B, FcmPushType.NEW_CHILD_COMMENTS,
                W, FcmPushType.NEW_COMMENTS));
        verify(newComment).mention(REPLY_ID);
    }

    @Test
    @DisplayName("태그 답글: 게시글 작성자가 최상위 댓글 작성자여도 답글 알림을 안 받으므로 게시글 알림(NEW_COMMENTS)을 받는다")
    void mention_boardWriterIsTopAuthor_getsCommentNotice() {
        givenThread(W, W, comment(REPLY_ID, B, TOP_ID));
        givenCreated();

        write(REPLY_ID);

        assertThat(published(2)).containsExactlyInAnyOrderEntriesOf(Map.of(
                B, FcmPushType.NEW_CHILD_COMMENTS,
                W, FcmPushType.NEW_COMMENTS));
    }

    @Test
    @DisplayName("태그 답글: 태그된 사람이 게시글 작성자면 답글 알림 하나만")
    void mention_boardWriterIsMentioned_onlyReplyNotice() {
        givenThread(B, A, comment(REPLY_ID, B, TOP_ID));
        givenCreated();

        write(REPLY_ID);

        assertThat(published(1)).containsExactlyEntriesOf(Map.of(B, FcmPushType.NEW_CHILD_COMMENTS));
    }

    @Test
    @DisplayName("태그 답글: 내 대댓글에 답하면 답글 알림 없음 (최상위 작성자에게도 안 감)")
    void mention_self_noReplyNotice() {
        givenThread(W, A, comment(REPLY_ID, ME, TOP_ID));
        givenCreated();

        write(REPLY_ID);

        assertThat(published(1)).containsExactlyEntriesOf(Map.of(W, FcmPushType.NEW_COMMENTS));
    }

    @Test
    @DisplayName("태그 답글: 태그된 사람이 탈퇴했으면 답글 알림 없음")
    void mention_withdrawn_noReplyNotice() {
        CommunityComments reply = comment(REPLY_ID, B, TOP_ID);
        given(reply.getCreatedBy().getStatus()).willReturn(UserStatus.DELETED);
        givenThread(W, A, reply);
        givenCreated();

        write(REPLY_ID);

        assertThat(published(1)).containsExactlyEntriesOf(Map.of(W, FcmPushType.NEW_COMMENTS));
    }

    @Test
    @DisplayName("태그한 대댓글이 지워졌으면(소프트 삭제·행 없음) 태그 없는 답글처럼 최상위 댓글 작성자에게, 태그도 남기지 않는다")
    void mention_deleted_fallsBackToTopAuthor() {
        CommunityComments deleted = comment(REPLY_ID, B, TOP_ID);
        given(deleted.isDeleted()).willReturn(true);
        givenThread(W, A, deleted);
        givenCreated();

        write(REPLY_ID);

        assertThat(published(2)).containsExactlyInAnyOrderEntriesOf(Map.of(
                A, FcmPushType.NEW_CHILD_COMMENTS,
                W, FcmPushType.NEW_COMMENTS));
        verify(newComment, never()).mention(any());
    }

    @Test
    @DisplayName("태그한 댓글 id 가 없으면(지워져 행이 사라짐) 태그 없는 답글처럼")
    void mention_missing_fallsBackToTopAuthor() {
        givenThread(W, A, null);
        given(communityCommentsRepository.findById(999L)).willReturn(Optional.empty());
        givenCreated();

        write(999L);

        assertThat(published(2)).containsExactlyInAnyOrderEntriesOf(Map.of(
                A, FcmPushType.NEW_CHILD_COMMENTS,
                W, FcmPushType.NEW_COMMENTS));
    }

    @Test
    @DisplayName("최상위 댓글 자신을 태그로 보내면 태그 없는 답글과 같다")
    void mention_topItself_sameAsNoMention() {
        givenThread(W, A, null);
        givenCreated();

        write(TOP_ID);

        assertThat(published(2)).containsExactlyInAnyOrderEntriesOf(Map.of(
                A, FcmPushType.NEW_CHILD_COMMENTS,
                W, FcmPushType.NEW_COMMENTS));
        verify(newComment, never()).mention(any());
    }

    @Test
    @DisplayName("다른 스레드의 댓글을 태그하면 WRONG_PARAMETER 이고 저장·알림 없음")
    void mention_otherThread_rejected() {
        givenThread(W, A, comment(REPLY_ID, B, 555L));

        assertThatThrownBy(() -> write(REPLY_ID))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
        verify(communityCommentsRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("대댓글이 아닌 댓글(최상위)에 태그를 보내면 WRONG_PARAMETER")
    void mention_onTopLevelComment_rejected() {
        given(communityCommentsRepository.findById(0L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityCommentsService.createComment(
                new CommunityCommentsWriteRequest(CONTENT_ID, null, "본문", REPLY_ID)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
        verify(communityCommentsRepository, never()).save(any());
    }
}
