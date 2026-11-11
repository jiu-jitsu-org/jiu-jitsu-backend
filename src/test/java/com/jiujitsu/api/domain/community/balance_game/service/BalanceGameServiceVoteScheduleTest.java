package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
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
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 투표는 시작 시각부터 마감 시각까지만 (#195: 미리 등록한 다음 날 게임·버퍼 중 새 게임은 투표 불가)
@ExtendWith(MockitoExtension.class)
class BalanceGameServiceVoteScheduleTest {

    @InjectMocks private BalanceGameService balanceGameService;

    @Mock private BalanceGameRepository balanceGameRepository;
    @Mock private BalanceGameVoteRepository balanceGameVoteRepository;
    @Mock private BalanceGameFactory balanceGameFactory;
    @Mock private BalanceGameMapper balanceGameMapper;
    @Mock private CommunityCommentsService communityCommentsService;
    @Mock private ContentService contentService;
    @Mock private NoticeService noticeService;
    @Mock private AuthenticationFacade authenticationFacade;

    private void givenGame(LocalDateTime startAt, LocalDateTime endAt) {
        BalanceGame game = BalanceGame.builder()
                .id(1L)
                .content(Content.builder().id(10L).contentType(ContentType.BALANCE).build())
                .optionAText("A").optionBText("B")
                .startAt(startAt).endAt(endAt).gameDate(LocalDate.now())
                .build();
        given(authenticationFacade.getCurrentUser()).willReturn(User.builder().id(1L).role(UserRole.USER).build());
        given(balanceGameRepository.findByContentId(10L)).willReturn(Optional.of(game));
    }

    @Test
    @DisplayName("시작 전이면 BALANCE_GAME_NOT_STARTED, 투표를 저장하지 않는다")
    void notStarted() {
        givenGame(LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(5));

        assertThatThrownBy(() -> balanceGameService.vote(10L, BalanceGameOption.A))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BALANCE_GAME_NOT_STARTED);
        verify(balanceGameVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("마감 뒤면 기존처럼 BALANCE_GAME_CLOSED")
    void closed() {
        givenGame(LocalDateTime.now().minusHours(5), LocalDateTime.now().minusHours(1));

        assertThatThrownBy(() -> balanceGameService.vote(10L, BalanceGameOption.A))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BALANCE_GAME_CLOSED);
    }

    @Test
    @DisplayName("진행 중이면 투표를 저장한다")
    void open() {
        givenGame(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        given(contentService.getContentLikeCount(any())).willReturn(Map.of());

        balanceGameService.vote(10L, BalanceGameOption.A);

        verify(balanceGameVoteRepository).save(any());
    }
}
