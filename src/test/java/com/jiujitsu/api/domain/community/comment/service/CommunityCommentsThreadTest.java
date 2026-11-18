package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.CommentThreadResponse;
import com.jiujitsu.api.domain.community.comment.dto.CommentsSortType;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.factory.CommentFactory;
import com.jiujitsu.api.domain.community.comment.factory.CommentLikeFactory;
import com.jiujitsu.api.domain.community.comment.mapper.CommentLikeMapper;
import com.jiujitsu.api.domain.community.comment.mapper.CommentMapper;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * 댓글 하나 — 알림에서 연 댓글 (#271)
 */
@ExtendWith(MockitoExtension.class)
class CommunityCommentsThreadTest {

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
    @Spy private CommentMapper commentMapper;
    @Mock private CommentLikeFactory commentLikeFactory;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private CommunityProfileService communityProfileService;

    private static final Long CONTENT_ID = 10L;
    private final Content content = content();
    private final User author = User.builder().id(1L).nickname("롤링중독").role(UserRole.USER).build();
    private final User blocked = User.builder().id(2L).nickname("차단된회원").role(UserRole.USER).build();

    @Test
    @DisplayName("댓글을 열면 comment 에 그 댓글(대댓글 미리보기 3개 · 대댓글 수), reply 는 null")
    void root() {
        CommunityComments root = comment(100L, 0L, author, 1);
        List<CommunityComments> replies = List.of(comment(101L, 100L, author, 2), comment(102L, 100L, author, 3),
                comment(103L, 100L, author, 4), comment(104L, 100L, author, 5));
        given(communityCommentsRepository.findById(100L)).willReturn(Optional.of(root));
        given(communityCommentsRepository.findRepliesByParentIds(List.of(100L))).willReturn(replies);

        CommentThreadResponse result = communityCommentsService.getCommentThread(100L, CommentsSortType.CREATE_ASC);

        assertThat(result.contentId()).isEqualTo(CONTENT_ID);
        assertThat(result.comment().id()).isEqualTo(100L);
        assertThat(result.comment().childCount()).isEqualTo(4);
        assertThat(result.comment().childrenList()).extracting(CommunityCommentsResponse::id).containsExactly(101L, 102L, 103L);
        assertThat(result.reply()).isNull();
    }

    @Test
    @DisplayName("대댓글을 열면 comment 에 원 댓글, reply 에 그 대댓글 (미리보기 3개 밖이어도)")
    void reply() {
        CommunityComments root = comment(100L, 0L, author, 1);
        CommunityComments target = comment(104L, 100L, author, 5);
        List<CommunityComments> replies = List.of(comment(101L, 100L, author, 2), comment(102L, 100L, author, 3),
                comment(103L, 100L, author, 4), target);
        given(communityCommentsRepository.findById(104L)).willReturn(Optional.of(target));
        given(communityCommentsRepository.findById(100L)).willReturn(Optional.of(root));
        given(communityCommentsRepository.findRepliesByParentIds(List.of(100L))).willReturn(replies);

        CommentThreadResponse result = communityCommentsService.getCommentThread(104L, CommentsSortType.CREATE_ASC);

        assertThat(result.comment().id()).isEqualTo(100L);
        assertThat(result.reply().id()).isEqualTo(104L);
        assertThat(result.comment().childrenList()).extracting(CommunityCommentsResponse::id).doesNotContain(104L);
    }

    @Test
    @DisplayName("차단한 작성자의 댓글은 목록처럼 마스킹(isBlocked), 내가 신고한 댓글은 isReported")
    void blockedAndReported_likeList() {
        CommunityComments target = comment(100L, 0L, blocked, 1);
        given(communityCommentsRepository.findById(100L)).willReturn(Optional.of(target));
        given(communityCommentsRepository.findRepliesByParentIds(List.of(100L))).willReturn(List.of());
        given(userBlockService.getBlockedUserIds()).willReturn(List.of(2L));
        given(reportService.getReportedTargetIds(ReportType.COMMENT)).willReturn(List.of(100L));

        CommunityCommentsResponse comment = communityCommentsService.getCommentThread(100L, null).comment();

        assertThat(comment.isBlocked()).isTrue();
        assertThat(comment.body()).isNull();
        assertThat(comment.isReported()).isTrue();
    }

    @Test
    @DisplayName("관리자가 숨긴 댓글 · 숨긴 원 댓글의 대댓글 · 없는 댓글은 COMMENT_NOT_FOUND (목록에 없음)")
    void hiddenOrMissing_notFound() {
        CommunityComments hidden = comment(100L, 0L, author, 1);
        lenient().when(hidden.getHiddenAt()).thenReturn(LocalDateTime.of(2026, 11, 1, 0, 0));
        CommunityComments replyOfHidden = comment(101L, 100L, author, 2);
        given(communityCommentsRepository.findById(100L)).willReturn(Optional.of(hidden));
        given(communityCommentsRepository.findById(101L)).willReturn(Optional.of(replyOfHidden));
        given(communityCommentsRepository.findById(999L)).willReturn(Optional.empty());

        for (Long id : List.of(100L, 101L, 999L)) {
            assertThatThrownBy(() -> communityCommentsService.getCommentThread(id, null))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("삭제된 댓글은 목록처럼 '삭제된 댓글'로 보임 (숨김이어도)")
    void deleted_listed() {
        CommunityComments deleted = comment(100L, 0L, author, 1);
        lenient().when(deleted.getDeletedAt()).thenReturn(LocalDateTime.of(2026, 11, 1, 0, 0));
        lenient().when(deleted.getHiddenAt()).thenReturn(LocalDateTime.of(2026, 11, 1, 0, 0));
        given(communityCommentsRepository.findById(100L)).willReturn(Optional.of(deleted));
        given(communityCommentsRepository.findRepliesByParentIds(List.of(100L))).willReturn(List.of());

        assertThat(communityCommentsService.getCommentThread(100L, null).comment().id()).isEqualTo(100L);
    }

    private static Content content() {
        Content content = mock(Content.class);
        lenient().when(content.getId()).thenReturn(CONTENT_ID);
        return content;
    }

    private CommunityComments comment(Long id, Long parentId, User writer, int minute) {
        CommunityComments comment = mock(CommunityComments.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 29, 12, minute);
        lenient().when(comment.getId()).thenReturn(id);
        lenient().when(comment.getParentId()).thenReturn(parentId);
        lenient().when(comment.getCreatedBy()).thenReturn(writer);
        lenient().when(comment.getContent()).thenReturn(content);
        lenient().when(comment.getBody()).thenReturn("본문");
        lenient().when(comment.getIsReported()).thenReturn(false);
        lenient().when(comment.getCreatedAt()).thenReturn(createdAt);
        lenient().when(comment.getUpdatedAt()).thenReturn(createdAt);
        return comment;
    }
}
