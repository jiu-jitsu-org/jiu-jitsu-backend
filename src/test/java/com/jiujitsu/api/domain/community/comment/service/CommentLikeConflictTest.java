package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.factory.CommentLikeFactory;
import com.jiujitsu.api.domain.community.comment.mapper.CommentLikeMapper;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 댓글 좋아요 저장이 무결성 오류로 실패했을 때 — 유니크 위반만 동시 요청으로 보고 지금 상태로 응답한다. (#199)
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
    @Spy private CommentLikeMapper commentLikeMapper = new CommentLikeMapper();

    @BeforeEach
    void setUp() {
        User me = mock(User.class);
        given(me.getId()).willReturn(1L);
        CommunityComments comment = mock(CommunityComments.class);
        given(comment.getId()).willReturn(100L);
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(communityCommentsRepository.findById(100L)).willReturn(Optional.of(comment));
        given(transactionTemplate.execute(any())).willAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        given(commentLikeRepository.existsByCommentIdAndCreatedById(100L, 1L)).willReturn(false);
    }

    @Test
    @DisplayName("유니크 위반이 아닌 무결성 오류(FK 등)는 성공으로 숨기지 않고 그대로 던진다")
    void otherIntegrityError_rethrows() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("could not execute statement",
                new SQLException("violates foreign key constraint", "23503"));
        given(commentLikeRepository.save(any())).willThrow(failure);

        assertThatThrownBy(() -> communityCommentsService.createCommentLike(new CommentLikeRequest(100L)))
                .isSameAs(failure);
        verify(transactionTemplate, times(1)).execute(any());   // 다시 읽지 않음
    }

    @Test
    @DisplayName("유니크 위반 뒤 다시 읽었을 때 그 사이 같은 사람의 다음 요청이 취소했으면 취소 상태로 응답한다 (500 아님)")
    void uniqueViolation_thenCancelledMeanwhile_respondsCurrentState() {
        given(commentLikeRepository.save(any())).willThrow(new DataIntegrityViolationException("could not execute statement",
                new SQLException("duplicate key value violates unique constraint", "23505")));
        given(commentLikeRepository.countGroupByCommentIds(List.of(100L))).willReturn(List.of());

        CommentLikeResponse response = communityCommentsService.createCommentLike(new CommentLikeRequest(100L));

        assertThat(response).isEqualTo(new CommentLikeResponse(100L, false, 0L));
    }
}
