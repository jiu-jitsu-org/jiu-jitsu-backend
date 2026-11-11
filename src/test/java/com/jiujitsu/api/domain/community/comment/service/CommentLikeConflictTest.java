package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeRequest;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.factory.CommentLikeFactory;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 좋아요 저장 중 무결성 오류가 났는데 좋아요가 없다면(동시 요청이 아닌 다른 원인) 성공으로 숨기지 않는다. (#199)
 */
@ExtendWith(MockitoExtension.class)
class CommentLikeConflictTest {

    @InjectMocks
    private CommunityCommentsService communityCommentsService;

    @Mock private TransactionTemplate transactionTemplate;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private CommunityCommentsRepository communityCommentsRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private CommentLikeFactory commentLikeFactory;

    @Test
    @DisplayName("저장이 무결성 오류로 실패했는데 재조회에 좋아요가 없으면 원래 예외를 그대로 던진다")
    void integrityErrorWithoutLike_rethrows() {
        User me = mock(User.class);
        given(me.getId()).willReturn(1L);
        CommunityComments comment = mock(CommunityComments.class);
        given(comment.getId()).willReturn(100L);
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(communityCommentsRepository.findById(100L)).willReturn(Optional.of(comment));
        given(transactionTemplate.execute(any())).willAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));

        DataIntegrityViolationException failure = new DataIntegrityViolationException("FK 위반");
        given(commentLikeRepository.existsByCommentIdAndCreatedById(100L, 1L)).willReturn(false);
        given(commentLikeRepository.save(any())).willThrow(failure);

        assertThatThrownBy(() -> communityCommentsService.createCommentLike(new CommentLikeRequest(100L)))
                .isSameAs(failure);
    }
}
