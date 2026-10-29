package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.CommentsListRequest;
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
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileInfo;
import com.jiujitsu.api.domain.community.profile.entity.BeltRank;
import com.jiujitsu.api.domain.community.profile.entity.BeltStripe;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 댓글 응답 작성자(author)에 띠·그랄·인증 여부 (#163)
 */
@ExtendWith(MockitoExtension.class)
class CommunityCommentsAuthorProfileTest {

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

    private final User owner = User.builder().id(1L).nickname("김관장").role(UserRole.OWNER).build();
    private final User noProfile = User.builder().id(2L).nickname("새벽반흰띠").role(UserRole.USER).build();
    private final User blocked = User.builder().id(3L).nickname("차단된회원").role(UserRole.USER).build();

    @Test
    @DisplayName("댓글·대댓글 작성자 프로필을 한 번에 조회해 author 에 띠·그랄·인증 여부를 담는다")
    void getComments_fillsAuthorBelt() {
        // given
        Content content = mock(Content.class);
        lenient().when(content.getId()).thenReturn(CONTENT_ID);
        CommunityComments root = comment(100L, 0L, owner, content, 1);
        CommunityComments child = comment(101L, 100L, noProfile, content, 2);
        given(contentRepository.findById(CONTENT_ID)).willReturn(Optional.of(content));
        given(communityCommentsRepository.findByContentIdFiltered(CONTENT_ID)).willReturn(List.of(root, child));
        CommunityProfile ownerProfile = CommunityProfile.builder()
                .user(owner).beltRank(BeltRank.BLACK).beltStripe(BeltStripe.STRIPE_1).build();
        given(communityProfileService.getProfilesByUserIds(Set.of(1L, 2L))).willReturn(Map.of(1L, ownerProfile));

        // when
        List<CommunityCommentsResponse> result = communityCommentsService.getComments(
                new CommentsListRequest(CONTENT_ID, CommentsSortType.CREATE_DESC));

        // then - 댓글 묶음당 1번
        verify(communityProfileService, times(1)).getProfilesByUserIds(any());
        CommunityProfileInfo rootAuthor = result.get(0).author();
        assertThat(rootAuthor.beltRank()).isEqualTo(BeltRank.BLACK);
        assertThat(rootAuthor.beltStripe()).isEqualTo(BeltStripe.STRIPE_1);
        assertThat(rootAuthor.isOwner()).isTrue();

        CommunityProfileInfo childAuthor = result.get(0).childrenList().get(0).author();
        assertThat(childAuthor.id()).isEqualTo(2L);
        assertThat(childAuthor.beltRank()).isNull();
        assertThat(childAuthor.isOwner()).isFalse();
    }

    @Test
    @DisplayName("차단한 작성자의 author 는 띠·그랄·인증 여부까지 모두 null")
    void getComments_blockedAuthorMasked() {
        // given
        Content content = mock(Content.class);
        lenient().when(content.getId()).thenReturn(CONTENT_ID);
        CommunityComments root = comment(100L, 0L, blocked, content, 1);
        given(contentRepository.findById(CONTENT_ID)).willReturn(Optional.of(content));
        given(userBlockService.getBlockedUserIds()).willReturn(List.of(blocked.getId()));
        given(communityCommentsRepository.findByContentIdFiltered(CONTENT_ID)).willReturn(List.of(root));
        CommunityProfile blockedProfile = CommunityProfile.builder()
                .user(blocked).beltRank(BeltRank.BLUE).beltStripe(BeltStripe.STRIPE_2).build();
        given(communityProfileService.getProfilesByUserIds(Set.of(3L))).willReturn(Map.of(3L, blockedProfile));

        // when
        List<CommunityCommentsResponse> result = communityCommentsService.getComments(
                new CommentsListRequest(CONTENT_ID, CommentsSortType.CREATE_DESC));

        // then
        CommunityProfileInfo author = result.get(0).author();
        assertThat(result.get(0).isBlocked()).isTrue();
        assertThat(author.id()).isNull();
        assertThat(author.beltRank()).isNull();
        assertThat(author.beltStripe()).isNull();
        assertThat(author.isOwner()).isNull();
    }

    private CommunityComments comment(Long id, Long parentId, User author, Content content, int minute) {
        CommunityComments comment = mock(CommunityComments.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 29, 12, minute);
        lenient().when(comment.getId()).thenReturn(id);
        lenient().when(comment.getParentId()).thenReturn(parentId);
        lenient().when(comment.getCreatedBy()).thenReturn(author);
        lenient().when(comment.getContent()).thenReturn(content);
        lenient().when(comment.getBody()).thenReturn("본문");
        lenient().when(comment.getIsReported()).thenReturn(false);
        lenient().when(comment.getCreatedAt()).thenReturn(createdAt);
        lenient().when(comment.getUpdatedAt()).thenReturn(createdAt);
        return comment;
    }
}
