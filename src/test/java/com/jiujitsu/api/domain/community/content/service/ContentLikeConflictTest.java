package com.jiujitsu.api.domain.community.content.service;

import com.jiujitsu.api.domain.community.content.dto.ContentLikeResponse;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.mapper.ContentLikeMapper;
import com.jiujitsu.api.domain.community.content.repository.ContentLikeRepository;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
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
 * 게시글 좋아요 저장이 무결성 오류로 실패했을 때 — 유니크 위반만 동시 요청으로 보고 지금 상태로 응답한다. (#199)
 */
@ExtendWith(MockitoExtension.class)
class ContentLikeConflictTest {

    @InjectMocks
    private ContentService contentService;

    @Mock private TransactionTemplate transactionTemplate;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private ContentRepository contentRepository;
    @Mock private ContentLikeRepository contentLikeRepository;
    @Spy private ContentLikeMapper contentLikeMapper = new ContentLikeMapper();

    private final Content content = mock(Content.class);

    @BeforeEach
    void setUp() {
        User me = mock(User.class);
        given(me.getId()).willReturn(1L);
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(contentRepository.findById(10L)).willReturn(Optional.of(content));
        given(transactionTemplate.execute(any())).willAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        given(contentLikeRepository.existsByContentIdAndCreatedById(10L, 1L)).willReturn(false);
    }

    @Test
    @DisplayName("유니크 위반이 아닌 무결성 오류(FK 등)는 성공으로 숨기지 않고 그대로 던진다")
    void otherIntegrityError_rethrows() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("could not execute statement",
                new SQLException("violates foreign key constraint", "23503"));
        given(contentLikeRepository.save(any())).willThrow(failure);

        assertThatThrownBy(() -> contentService.like(10L)).isSameAs(failure);
        verify(transactionTemplate, times(1)).execute(any());   // 다시 읽지 않음
    }

    @Test
    @DisplayName("유니크 위반 뒤 다시 읽었을 때 그 사이 같은 사람의 다음 요청이 취소했으면 취소 상태로 응답한다 (500 아님)")
    void uniqueViolation_thenCancelledMeanwhile_respondsCurrentState() {
        given(contentLikeRepository.save(any())).willThrow(new DataIntegrityViolationException("could not execute statement",
                new SQLException("duplicate key value violates unique constraint", "23505")));
        given(contentLikeRepository.countContentLikeByContentIds(List.of(10L))).willReturn(List.of());
        given(content.getId()).willReturn(10L);

        ContentLikeResponse response = contentService.like(10L);

        assertThat(response).isEqualTo(new ContentLikeResponse(10L, false, 0L));
    }
}
