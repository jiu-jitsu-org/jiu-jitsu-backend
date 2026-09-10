package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameRepository;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BalanceGameServiceViewCountTest {

    private static final Long CONTENT_ID = 1L;

    @InjectMocks
    private BalanceGameService balanceGameService;

    @Mock private BalanceGameRepository balanceGameRepository;
    @Mock private BalanceGameVoteRepository balanceGameVoteRepository;
    @Mock private BalanceGameFactory balanceGameFactory;
    @Mock private BalanceGameMapper balanceGameMapper;
    @Mock private CommunityCommentsService communityCommentsService;
    @Mock private ContentService contentService;
    @Mock private NoticeService noticeService;
    @Mock private AuthenticationFacade authenticationFacade;

    private BalanceGame game() {
        Content content = Content.builder()
                .id(CONTENT_ID)
                .contentType(ContentType.BALANCE)
                .build();
        return BalanceGame.builder()
                .id(10L)
                .content(content)
                .optionAText("옵션A")
                .optionBText("옵션B")
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    // toResponseWithVotes 가 호출하는 집계 조회 스텁 (조회수 검증에는 값 자체가 의미 없어 0으로 고정)
    private void stubAggregations() {
        given(balanceGameVoteRepository.countByBalanceGameIdAndOption(anyLong(), any())).willReturn(0L);
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.empty());
        given(communityCommentsService.getCountComments(CONTENT_ID)).willReturn(0L);
        given(contentService.getContentLikeCount(any())).willReturn(Map.of());
        given(balanceGameMapper.toResponse(any(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), any(), anyLong(), anyLong(), any(), any()))
                .willReturn(null);
    }

    @Test
    @DisplayName("상세 조회 시 조회수가 1 증가한다")
    void getDetail_incrementsViewCount() {
        // given
        BalanceGame game = game();
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(game));
        stubAggregations();

        // when
        balanceGameService.getDetail(CONTENT_ID);

        // then
        assertThat(game.getContent().getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("메인의 '오늘의 게임' 조회는 조회수를 증가시키지 않는다")
    void getCurrent_doesNotIncrementViewCount() {
        // given
        BalanceGame game = game();
        given(balanceGameRepository.findTopByEndAtAfterOrderByEndAtAsc(any())).willReturn(Optional.of(game));
        stubAggregations();

        // when
        balanceGameService.getCurrent();

        // then
        assertThat(game.getContent().getViewCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 밸런스 게임 상세 조회 시 예외가 발생한다")
    void getDetail_notFound_throwsException() {
        // given
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> balanceGameService.getDetail(CONTENT_ID))
                .isInstanceOf(ErrorException.class)
                .extracting(e -> ((ErrorException) e).getErrorCode())
                .isEqualTo(ErrorCode.BALANCE_GAME_NOT_FOUND);
    }
}
