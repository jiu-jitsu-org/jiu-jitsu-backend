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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * '결과 봤음' 기록(result-views)과 조회 응답 네 곳의 resultSeen (#290)
 */
@ExtendWith(MockitoExtension.class)
class BalanceGameServiceResultSeenTest {

    private static final Long GAME_ID = 10L;
    private static final Long CONTENT_ID = 100L;

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

    private final User user = new User();

    @BeforeEach
    void setUp() {
        user.setId(7L);
    }

    private BalanceGame game(LocalDateTime startAt, LocalDateTime endAt) {
        return BalanceGame.builder()
                .id(GAME_ID)
                .content(Content.builder().id(CONTENT_ID).contentType(ContentType.BALANCE).build())
                .optionAText("옵션A")
                .optionBText("옵션B")
                .startAt(startAt)
                .endAt(endAt)
                .gameDate(startAt.toLocalDate())
                .build();
    }

    private BalanceGame closedGame() {
        LocalDateTime now = LocalDateTime.now();
        return game(now.minusHours(23), now.minusMinutes(1));
    }

    private BalanceGame openGame() {
        LocalDateTime now = LocalDateTime.now();
        return game(now.minusHours(1), now.plusHours(1));
    }

    private BalanceGameVote vote(BalanceGame game, boolean seen) {
        return BalanceGameVote.builder()
                .balanceGame(game)
                .option(BalanceGameOption.A)
                .resultSeenAt(seen ? LocalDateTime.now() : null)
                .build();
    }

    private void givenMyVote(BalanceGameVote vote) {
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.of(user));
        given(balanceGameVoteRepository.findByBalanceGameIdAndCreatedBy(GAME_ID, user)).willReturn(Optional.ofNullable(vote));
    }

    // ---- 기록

    @Test
    @DisplayName("끝난 판이면 내 투표에 지금 시각을 남긴다")
    void mark_closed() {
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(closedGame()));
        LocalDateTime before = LocalDateTime.now();

        balanceGameService.markResultSeen(CONTENT_ID);

        ArgumentCaptor<LocalDateTime> seenAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(balanceGameVoteRepository).markResultSeen(eq(GAME_ID), eq(user), seenAt.capture());
        assertThat(seenAt.getValue()).isBetween(before, LocalDateTime.now());
        verify(noticeService).readBalanceResultNotice(7L, CONTENT_ID);   // 그 판의 결과 알림도 읽음 (#291)
    }

    @Test
    @DisplayName("투표하지 않았거나 이미 본 판(0행)이어도 오류 없이 끝난다")
    void mark_nothingToUpdate() {
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(closedGame()));
        given(balanceGameVoteRepository.markResultSeen(eq(GAME_ID), eq(user), any())).willReturn(0);

        balanceGameService.markResultSeen(CONTENT_ID);

        verifyNoMoreInteractions(balanceGameVoteRepository);
    }

    @Test
    @DisplayName("진행 중인 판은 남기지 않고 넘어간다")
    void mark_open() {
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(openGame()));

        balanceGameService.markResultSeen(CONTENT_ID);

        verifyNoInteractions(balanceGameVoteRepository);
    }

    @Test
    @DisplayName("시작 전인 판은 남기지 않고 넘어간다")
    void mark_notStarted() {
        LocalDateTime now = LocalDateTime.now();
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(game(now.plusHours(1), now.plusHours(5))));

        balanceGameService.markResultSeen(CONTENT_ID);

        verifyNoInteractions(balanceGameVoteRepository);
    }

    @Test
    @DisplayName("없는 판이면 BALANCE_GAME_NOT_FOUND")
    void mark_notFound() {
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> balanceGameService.markResultSeen(CONTENT_ID))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BALANCE_GAME_NOT_FOUND);
        verifyNoInteractions(balanceGameVoteRepository);
    }

    @Test
    @DisplayName("비로그인은 LOGIN_NOT_ACCESS, 아무것도 조회·기록하지 않는다")
    void mark_notLoggedIn() {
        given(authenticationFacade.getCurrentUser()).willThrow(new ErrorException(ErrorCode.LOGIN_NOT_ACCESS));

        assertThatThrownBy(() -> balanceGameService.markResultSeen(CONTENT_ID))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_NOT_ACCESS);
        verifyNoInteractions(balanceGameRepository, balanceGameVoteRepository);
    }

    // ---- 조회 응답

    @ParameterizedTest(name = "[{index}] seen={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("오늘의 게임 — 버퍼 시간의 끝난 판에 투표했으면 봤음 여부")
    void current(boolean seen) {
        BalanceGame game = closedGame();
        given(balanceGameRepository.findOpen(any(), any())).willReturn(List.of());
        given(balanceGameRepository.findClosed(any(), any())).willReturn(List.of(game));
        givenMyVote(vote(game, seen));

        assertThat(balanceGameService.getCurrent().resultSeen()).isEqualTo(seen);
    }

    @ParameterizedTest(name = "[{index}] seen={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("상세 — 끝난 판에 투표했으면 봤음 여부")
    void detail(boolean seen) {
        BalanceGame game = closedGame();
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(game));
        givenMyVote(vote(game, seen));

        assertThat(balanceGameService.getDetail(CONTENT_ID).resultSeen()).isEqualTo(seen);
    }

    @Test
    @DisplayName("상세 — 끝난 판이어도 투표하지 않았으면 null")
    void detail_notVoted() {
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(closedGame()));
        givenMyVote(null);

        assertThat(balanceGameService.getDetail(CONTENT_ID).resultSeen()).isNull();
    }

    @Test
    @DisplayName("상세 — 비로그인은 null")
    void detail_guest() {
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(closedGame()));
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.empty());

        assertThat(balanceGameService.getDetail(CONTENT_ID).resultSeen()).isNull();
    }

    @Test
    @DisplayName("투표 — 진행 중인 판이라 null")
    void vote_open() {
        BalanceGame game = openGame();
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(game));
        givenMyVote(vote(game, false));

        BalanceGameResponse response = balanceGameService.vote(CONTENT_ID, BalanceGameOption.B);

        assertThat(response.myVote()).isEqualTo(BalanceGameOption.B);
        assertThat(response.resultSeen()).isNull();
    }

    @Test
    @DisplayName("내가 투표한 목록 — 끝난 판은 봤음 여부, 진행 중인 판은 null. 목록 한 번 · 득표수 한 번 말고 투표를 더 읽지 않는다")
    void participated() {
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameVoteRepository.findMyVotes(eq(user), any())).willReturn(new PageImpl<>(List.of(
                vote(openGame(), false), vote(closedGame(), true), vote(closedGame(), false))));

        assertThat(balanceGameService.getParticipatedList(PageRequest.of(0, 20)).getContent())
                .extracting(BalanceGameResponse::resultSeen)
                .containsExactly(null, true, false);
        verify(balanceGameVoteRepository).findMyVotes(eq(user), any());
        verify(balanceGameVoteRepository).countByBalanceGameIdsGroupByOption(any());
        verifyNoMoreInteractions(balanceGameVoteRepository);
    }
}
