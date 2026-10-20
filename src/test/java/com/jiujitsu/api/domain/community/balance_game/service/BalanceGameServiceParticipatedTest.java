package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameRepository;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BalanceGameServiceParticipatedTest {

    private static final Long USER_ID = 7L;

    @InjectMocks
    private BalanceGameService balanceGameService;

    @Mock private BalanceGameRepository balanceGameRepository;
    @Mock private BalanceGameVoteRepository balanceGameVoteRepository;
    @Mock private BalanceGameFactory balanceGameFactory;
    @Spy private BalanceGameMapper balanceGameMapper = new BalanceGameMapper();
    @Mock private CommunityCommentsService communityCommentsService;
    @Mock private ContentService contentService;
    @Mock private NoticeService noticeService;
    @Mock private AuthenticationFacade authenticationFacade;

    private User user() {
        User user = new User();
        user.setId(USER_ID);
        return user;
    }

    private BalanceGame game(Long id, Long contentId, LocalDateTime endAt) {
        Content content = Content.builder()
                .id(contentId)
                .contentType(ContentType.BALANCE)
                .build();
        return BalanceGame.builder()
                .id(id)
                .content(content)
                .optionAText("옵션A-" + id)
                .optionBText("옵션B-" + id)
                .endAt(endAt)
                .gameDate(endAt.toLocalDate())
                .build();
    }

    private BalanceGameVote vote(BalanceGame game, BalanceGameOption option) {
        return BalanceGameVote.builder().balanceGame(game).option(option).build();
    }

    @Test
    @DisplayName("투표한 게임마다 상세와 같은 지표를 채워 반환하고, 조회수는 올리지 않는다")
    void getParticipatedList() {
        // given
        User user = user();
        BalanceGame current = game(10L, 100L, LocalDateTime.now().plusHours(1));
        BalanceGame closed = game(11L, 101L, LocalDateTime.now().minusDays(1));
        Pageable pageable = PageRequest.of(0, 20);

        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameVoteRepository.findMyVotes(eq(user), any()))
                .willReturn(new PageImpl<>(List.of(vote(current, BalanceGameOption.B), vote(closed, BalanceGameOption.A)), pageable, 2));
        given(balanceGameVoteRepository.countByBalanceGameIdsGroupByOption(List.of(10L, 11L)))
                .willReturn(List.of(
                        new Object[]{10L, BalanceGameOption.A, 3L},
                        new Object[]{10L, BalanceGameOption.B, 2L},
                        new Object[]{11L, BalanceGameOption.A, 1L}));
        List<Long> contentIds = List.of(100L, 101L);
        given(communityCommentsService.getContentsComments(contentIds)).willReturn(Map.of(100L, 4L));
        given(contentService.getContentLikeCount(contentIds)).willReturn(Map.of(101L, 9L));
        given(communityCommentsService.getUserCommentedContentIds(USER_ID, contentIds)).willReturn(Set.of(100L));
        given(contentService.getUserLikedContentIds(USER_ID, contentIds)).willReturn(Set.of(101L));
        given(noticeService.getNoticeDisabledContentIds(USER_ID, contentIds)).willReturn(Set.of(101L));

        // when
        Page<BalanceGameResponse> result = balanceGameService.getParticipatedList(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);

        BalanceGameResponse first = result.getContent().get(0);
        assertThat(first.contentId()).isEqualTo(100L);
        assertThat(first.closed()).isFalse();
        assertThat(first.myVote()).isEqualTo(BalanceGameOption.B);
        assertThat(first.optionA().voteCount()).isEqualTo(3L);
        assertThat(first.optionB().voteCount()).isEqualTo(2L);
        assertThat(first.totalVoteCount()).isEqualTo(5L);
        assertThat(first.commentCount()).isEqualTo(4L);
        assertThat(first.isCommented()).isTrue();
        assertThat(first.likeCount()).isZero();
        assertThat(first.isLiked()).isFalse();
        assertThat(first.noticeEnabled()).isTrue();

        BalanceGameResponse second = result.getContent().get(1);
        assertThat(second.contentId()).isEqualTo(101L);
        assertThat(second.closed()).isTrue();
        assertThat(second.myVote()).isEqualTo(BalanceGameOption.A);
        assertThat(second.optionB().voteCount()).isZero();
        assertThat(second.totalVoteCount()).isEqualTo(1L);
        assertThat(second.commentCount()).isZero();
        assertThat(second.isCommented()).isFalse();
        assertThat(second.likeCount()).isEqualTo(9L);
        assertThat(second.isLiked()).isTrue();
        assertThat(second.noticeEnabled()).isFalse();

        assertThat(current.getContent().getViewCount()).isZero();
        assertThat(closed.getContent().getViewCount()).isZero();
    }

    @Test
    @DisplayName("클라이언트가 보낸 sort 는 제거하고 페이지 번호·크기만 넘긴다")
    void getParticipatedList_dropsClientSort() {
        // given
        User user = user();
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameVoteRepository.findMyVotes(eq(user), any())).willReturn(Page.empty());

        // when
        balanceGameService.getParticipatedList(PageRequest.of(2, 5, Sort.by("createdAt")));

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(balanceGameVoteRepository).findMyVotes(eq(user), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("투표한 게임이 없으면 집계 쿼리 없이 빈 페이지를 반환한다")
    void getParticipatedList_empty() {
        // given
        User user = user();
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameVoteRepository.findMyVotes(eq(user), any())).willReturn(Page.empty());

        // when
        Page<BalanceGameResponse> result = balanceGameService.getParticipatedList(PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
        verify(balanceGameVoteRepository, never()).countByBalanceGameIdsGroupByOption(anyList());
        verifyNoInteractions(communityCommentsService, contentService, noticeService);
    }

    @Test
    @DisplayName("비로그인 사용자는 LOGIN_NOT_ACCESS 예외가 발생한다")
    void getParticipatedList_notLoggedIn() {
        // given
        given(authenticationFacade.getCurrentUser()).willThrow(new ErrorException(ErrorCode.LOGIN_NOT_ACCESS));

        // when & then
        assertThatThrownBy(() -> balanceGameService.getParticipatedList(PageRequest.of(0, 20)))
                .isInstanceOf(ErrorException.class)
                .extracting(e -> ((ErrorException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_NOT_ACCESS);
        verifyNoInteractions(balanceGameVoteRepository);
    }
}
