package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeRequest;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.factory.CommentLikeFactory;
import com.jiujitsu.api.domain.community.comment.mapper.CommentLikeMapper;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.notice.event.LikeNoticeEvent;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 댓글 좋아요 푸시 data 에 좋아요 받은 댓글 id (#271)
 */
@ExtendWith(MockitoExtension.class)
class CommentLikeNoticeDataTest {

    @InjectMocks
    private CommunityCommentsService communityCommentsService;

    @Mock private TransactionTemplate transactionTemplate;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private CommunityCommentsRepository communityCommentsRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private CommentLikeFactory commentLikeFactory;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Spy private CommentLikeMapper commentLikeMapper = new CommentLikeMapper();

    @Test
    @DisplayName("남의 댓글에 좋아요 → LikeNoticeEvent data 에 contentId 와 commentId")
    void like_carriesCommentId() {
        User me = mock(User.class);
        given(me.getId()).willReturn(1L);
        User author = mock(User.class);
        given(author.getId()).willReturn(2L);
        Content content = mock(Content.class);
        given(content.getId()).willReturn(10L);
        given(content.getContentType()).willReturn(ContentType.BOARD);
        CommunityComments comment = mock(CommunityComments.class);
        given(comment.getId()).willReturn(100L);
        given(comment.getCreatedBy()).willReturn(author);
        given(comment.getContent()).willReturn(content);
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(communityCommentsRepository.findById(100L)).willReturn(Optional.of(comment));
        given(transactionTemplate.execute(any())).willAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        given(commentLikeRepository.existsByCommentIdAndCreatedById(100L, 1L)).willReturn(false);
        given(commentLikeRepository.countGroupByCommentIds(List.of(100L))).willReturn(List.of());

        communityCommentsService.createCommentLike(new CommentLikeRequest(100L));

        ArgumentCaptor<LikeNoticeEvent> captor = ArgumentCaptor.forClass(LikeNoticeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().pushData()).containsEntry("data", "10").containsEntry("commentId", "100");
    }
}
