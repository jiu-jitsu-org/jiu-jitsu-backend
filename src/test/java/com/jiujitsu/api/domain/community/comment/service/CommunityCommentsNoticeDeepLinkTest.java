package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsWriteRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeRequest;
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
import com.jiujitsu.api.domain.notice.event.LikeNoticeEvent;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 댓글 알림이 대상 컨텐츠 타입을 따르는지 검증한다.
 * - 딥링크: 게시글 BOARD_DETAIL / 밸런스 게임 BALANCE_DETAIL (#120)
 * - 받는 사람: 내 게시글의 내 댓글엔 대댓글 알림 1건 (#177), 밸런스 게임은 등록한 관리자에게 글쓴이 알림 없음 (#196)
 */
@ExtendWith(MockitoExtension.class)
class CommunityCommentsNoticeDeepLinkTest {

    @InjectMocks
    private CommunityCommentsService communityCommentsService;

    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private CommunityCommentsRepository communityCommentsRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private UserBlockService userBlockService;
    @Mock private ReportService reportService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private CommentFactory commentFactory;
    @Mock private CommentMapper commentMapper;
    @Mock private CommentLikeFactory commentLikeFactory;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private CommunityProfileService communityProfileService;
    @Mock private TransactionTemplate transactionTemplate;

    private static final Long CONTENT_ID = 10L;
    private static final Long ME_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long THIRD_ID = 3L;

    @Test
    @DisplayName("밸런스 게임 대댓글은 댓글 작성자에게만 BALANCE_DETAIL 로 나간다 — 게임을 등록한 관리자에게 \"내 게시글에 댓글\" 알림 없음 (#196)")
    void createComment_onBalance_notifiesOnlyParentAuthorWithBalanceDetail() {
        // given
        Content content = stubContent(ContentType.BALANCE);
        // 게임을 등록한 관리자는 알림 대상이 아니라 스텁하지 않는다
        CommunityComments parent = stubComment(100L, THIRD_ID); // 글쓴이와 다른 사람의 댓글 (#177)
        stubCreateComment(content, parent);

        // when
        communityCommentsService.createComment(new CommunityCommentsWriteRequest(CONTENT_ID, 100L, "대댓글", null));

        // then - 부모 댓글 작성자 알림 한 건만, 밸런스 상세로 향한다
        CommentNoticeEvent event = capturePublishedEvents(1).get(0);
        assertThat(event.pushType()).isEqualTo(FcmPushType.NEW_CHILD_COMMENTS);
        assertThat(event.recipientUserId()).isEqualTo(THIRD_ID);
        assertThat(event.pushData().get("type")).isEqualTo("BALANCE_DETAIL");
    }

    @Test
    @DisplayName("밸런스 게임에 댓글: 알림 없음 — 운영자가 올린 게임이라 등록한 관리자 계정에 보내지 않음 (#196)")
    void createComment_topLevelOnBalance_noNotice() {
        // given - 대댓글이 아닌 댓글 (부모 없음)
        Content content = mock(Content.class);
        given(content.getContentType()).willReturn(ContentType.BALANCE);
        given(authenticationFacade.getCurrentUser()).willReturn(mock(User.class));
        given(contentRepository.findById(CONTENT_ID)).willReturn(Optional.of(content));
        given(commentFactory.createComments(any(), any(), any(), any())).willReturn(mock(CommunityComments.class));

        // when
        communityCommentsService.createComment(new CommunityCommentsWriteRequest(CONTENT_ID, 0L, "가드 패스가 낫죠", null));

        // then
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("게시글 대댓글 알림은 기존대로 BOARD_DETAIL 로 나간다")
    void createComment_onBoard_publishesBoardDetail() {
        // given
        Content content = stubContent(ContentType.BOARD);
        stubContentWriter(content);
        CommunityComments parent = stubComment(100L, THIRD_ID); // 글쓴이와 다른 사람의 댓글 (#177)
        stubCreateComment(content, parent);

        // when
        communityCommentsService.createComment(new CommunityCommentsWriteRequest(CONTENT_ID, 100L, "대댓글", null));

        // then
        assertThat(capturePublishedEvents(2))
                .extracting(e -> e.pushData().get("type"))
                .containsOnly("BOARD_DETAIL");
    }

    @Test
    @DisplayName("내 게시글의 내 댓글에 대댓글이 달리면 대댓글 알림 1건만 나간다 (#177)")
    void createComment_onOwnCommentInOwnPost_publishesOnlyChildNotice() {
        // given - 게시글 작성자와 부모 댓글 작성자가 같은 사람
        Content content = stubContent(ContentType.BOARD);
        stubContentWriter(content);
        CommunityComments parent = stubComment(100L, OTHER_ID);
        stubCreateComment(content, parent);

        // when
        communityCommentsService.createComment(new CommunityCommentsWriteRequest(CONTENT_ID, 100L, "대댓글", null));

        // then
        CommentNoticeEvent event = capturePublishedEvents(1).get(0);
        assertThat(event.pushType()).isEqualTo(FcmPushType.NEW_CHILD_COMMENTS);
        assertThat(event.recipientUserId()).isEqualTo(OTHER_ID);
    }

    @Test
    @DisplayName("밸런스 게임 댓글 좋아요 알림은 BALANCE_DETAIL 로 나간다")
    void createCommentLike_onBalance_publishesBalanceDetail() {
        // given
        Content content = stubContent(ContentType.BALANCE);
        CommunityComments comment = stubComment(100L, OTHER_ID);
        given(comment.getContent()).willReturn(content);

        User me = stubUser(ME_ID);
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(communityCommentsRepository.findById(100L)).willReturn(Optional.of(comment));
        given(commentLikeRepository.existsByCommentIdAndCreatedById(any(), any())).willReturn(false);
        givenTransactionRunsInline();
        given(commentLikeRepository.countGroupByCommentIds(anyList())).willReturn(List.of());

        // when
        communityCommentsService.createCommentLike(new CommentLikeRequest(100L));

        // then - 좋아요는 개수 기준 알림 이벤트로 나간다 (#196)
        ArgumentCaptor<LikeNoticeEvent> captor = ArgumentCaptor.forClass(LikeNoticeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        LikeNoticeEvent event = captor.getValue();
        assertThat(event.pushType()).isEqualTo(FcmPushType.COMMENTS_LIKE);
        assertThat(event.recipientUserId()).isEqualTo(OTHER_ID);
        // 카드는 좋아요 받은 댓글 기준 한 장
        assertThat(event.commentId()).isEqualTo(100L);
        assertThat(event.groupKey()).isEqualTo("COMMENTS_LIKE:100");
        assertThat(event.pushData().get("type")).isEqualTo("BALANCE_DETAIL");
        // 상세 진입에 쓰이는 data 는 commentId 가 아니라 contentId 여야 한다 (#136)
        assertThat(event.pushData().get("data")).isEqualTo(CONTENT_ID.toString());
        // 알림 수신 설정 조회용 contentId 는 댓글이 속한 컨텐츠여야 한다
        assertThat(event.contentId()).isEqualTo(CONTENT_ID);
    }

    // 좋아요는 TransactionTemplate 안에서 실행된다 (#199) → 콜백을 그대로 실행
    private void givenTransactionRunsInline() {
        given(transactionTemplate.execute(any())).willAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    private void stubCreateComment(Content content, CommunityComments parent) {
        User me = stubUser(ME_ID);
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(contentRepository.findById(CONTENT_ID)).willReturn(Optional.of(content));
        given(communityCommentsRepository.findById(parent.getId())).willReturn(Optional.of(parent));
        given(commentFactory.createComments(any(), any(), any(), any())).willReturn(mock(CommunityComments.class));
    }

    private Content stubContent(ContentType contentType) {
        Content content = mock(Content.class);
        given(content.getId()).willReturn(CONTENT_ID);
        given(content.getContentType()).willReturn(contentType);
        return content;
    }

    private void stubContentWriter(Content content) {
        // 스터빙 중첩(UnfinishedStubbing) 을 피하려고 User 목은 미리 만들어 둔다
        User writer = stubUser(OTHER_ID);
        given(content.getCreatedBy()).willReturn(writer);
    }

    private CommunityComments stubComment(Long id, Long writerId) {
        User writer = stubUser(writerId);
        CommunityComments comment = mock(CommunityComments.class);
        given(comment.getId()).willReturn(id);
        given(comment.getCreatedBy()).willReturn(writer);
        return comment;
    }

    private User stubUser(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private List<CommentNoticeEvent> capturePublishedEvents(int expectedCount) {
        ArgumentCaptor<CommentNoticeEvent> captor = ArgumentCaptor.forClass(CommentNoticeEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(expectedCount)).publishEvent(captor.capture());
        return captor.getAllValues();
    }
}
