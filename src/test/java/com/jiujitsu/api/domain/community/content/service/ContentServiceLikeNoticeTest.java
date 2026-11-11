package com.jiujitsu.api.domain.community.content.service;

import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.mapper.ContentLikeMapper;
import com.jiujitsu.api.domain.community.content.repository.ContentLikeRepository;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.content.repository.ContentSaveRepository;
import com.jiujitsu.api.domain.community.content.mapper.ContentSaveMapper;
import com.jiujitsu.api.domain.notice.event.LikeNoticeEvent;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// 게시글 좋아요 (#196): 알림은 바로 보내지 않고 개수 기준 처리 이벤트만 발행한다
@ExtendWith(MockitoExtension.class)
class ContentServiceLikeNoticeTest {

    @InjectMocks private ContentService contentService;

    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private ContentRepository contentRepository;
    @Mock private ContentLikeRepository contentLikeRepository;
    @Mock private ContentLikeMapper contentLikeMapper;
    @Mock private ContentSaveRepository contentSaveRepository;
    @Mock private ContentSaveMapper contentSaveMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TransactionTemplate transactionTemplate;

    private final User me = User.builder().id(1L).role(UserRole.USER).build();
    private final User writer = User.builder().id(2L).role(UserRole.USER).build();

    // 좋아요는 TransactionTemplate 안에서 실행된다 (#199) → 콜백을 그대로 실행
    @BeforeEach
    void runTransactionInline() {
        given(transactionTemplate.execute(any())).willAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    private Content content(User owner, ContentType type) {
        Content content = Content.builder().id(10L).contentType(type).build();
        org.springframework.test.util.ReflectionTestUtils.setField(content, "createdBy", owner);
        return content;
    }

    @Test
    @DisplayName("남의 게시글에 좋아요: 글쓴이에게 CONTENTS_LIKE 이벤트 (딥링크 BOARD_DETAIL)")
    void like_publishesLikeNoticeEvent() {
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(contentRepository.findById(10L)).willReturn(Optional.of(content(writer, ContentType.BOARD)));
        given(contentLikeRepository.existsByContentIdAndCreatedById(10L, 1L)).willReturn(false);
        given(contentLikeRepository.countContentLikeByContentIds(List.of(10L))).willReturn(List.of());

        contentService.like(10L);

        ArgumentCaptor<LikeNoticeEvent> captor = ArgumentCaptor.forClass(LikeNoticeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        LikeNoticeEvent event = captor.getValue();
        assertThat(event.recipientUserId()).isEqualTo(2L);
        assertThat(event.contentId()).isEqualTo(10L);
        assertThat(event.commentId()).isNull();
        assertThat(event.pushType()).isEqualTo(FcmPushType.CONTENTS_LIKE);
        assertThat(event.pushData()).isEqualTo(Map.of("type", "BOARD_DETAIL", "data", "10"));
        assertThat(event.groupKey()).isEqualTo("CONTENTS_LIKE:10");
    }

    @Test
    @DisplayName("밸런스 게임 좋아요는 알림 없음 — 운영자가 올린 게임이라 등록한 관리자 계정에 \"내 게시글\" 알림이 가면 안 됨")
    void likeBalanceGame_noEvent() {
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(contentRepository.findById(10L)).willReturn(Optional.of(content(writer, ContentType.BALANCE)));
        given(contentLikeRepository.existsByContentIdAndCreatedById(10L, 1L)).willReturn(false);
        given(contentLikeRepository.countContentLikeByContentIds(List.of(10L))).willReturn(List.of());

        contentService.like(10L);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("내 글에 좋아요는 알림 없음")
    void likeOwnContent_noEvent() {
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(contentRepository.findById(10L)).willReturn(Optional.of(content(me, ContentType.BOARD)));
        given(contentLikeRepository.existsByContentIdAndCreatedById(10L, 1L)).willReturn(false);
        given(contentLikeRepository.countContentLikeByContentIds(List.of(10L))).willReturn(List.of());

        contentService.like(10L);

        verifyNoInteractions(eventPublisher);
    }
}
