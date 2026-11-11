package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 메인 '오늘의 게임': 진행 중 게임 → 없으면 결과 노출 시간 안의 최근 마감 게임 + resultUntil (#195)
// 쿼리 경계(시작·마감 포함 여부, 정렬)는 BalanceGameRepositoryTest, 여기선 고르는 규칙과 응답(closed·resultUntil·serverTime)을 본다
@ExtendWith(MockitoExtension.class)
class BalanceGameServiceCurrentTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    @InjectMocks private BalanceGameService balanceGameService;

    @Mock private BalanceGameRepository balanceGameRepository;
    @Mock private BalanceGameVoteRepository balanceGameVoteRepository;
    @Mock private BalanceGameFactory balanceGameFactory;
    @Spy private BalanceGameMapper balanceGameMapper = new BalanceGameMapper();
    @Mock private CommunityCommentsService communityCommentsService;
    @Mock private ContentService contentService;
    @Mock private NoticeService noticeService;
    @Mock private AuthenticationFacade authenticationFacade;

    private long nextId = 1;

    // 그날의 게임: 00:30 시작, 23:59:59 마감 (기본 일정)
    private BalanceGame dayGame(LocalDate day) {
        return game(day, BalanceGame.defaultStartAt(day));
    }

    private BalanceGame game(LocalDate day, LocalDateTime startAt) {
        long id = nextId++;
        return BalanceGame.builder()
                .id(id)
                .content(Content.builder().id(id).contentType(ContentType.BALANCE).build())
                .optionAText(day + " A")
                .optionBText(day + " B")
                .startAt(startAt)
                .endAt(day.atTime(23, 59, 59))
                .gameDate(day)
                .build();
    }

    private void givenOpen(BalanceGame game) {
        given(balanceGameRepository.findOpen(any(), any())).willReturn(List.of(game));
    }

    private void givenClosed(BalanceGame closed, BalanceGame upcoming) {
        given(balanceGameRepository.findClosed(any(), any())).willReturn(List.of(closed));
        given(balanceGameRepository.findUpcoming(any(), any())).willReturn(upcoming == null ? List.of() : List.of(upcoming));
    }

    @Test
    @DisplayName("진행 중 게임: closed=false, resultUntil=null, serverTime=now")
    void open() {
        LocalDateTime now = TODAY.atTime(12, 0);
        givenOpen(dayGame(TODAY));

        BalanceGameResponse response = balanceGameService.getCurrent(now);

        assertThat(response.optionA().text()).isEqualTo("2026-11-11 A");
        assertThat(response.started()).isTrue();
        assertThat(response.closed()).isFalse();
        assertThat(response.resultUntil()).isNull();
        assertThat(response.serverTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("마감 정각(23:59:59)에 고른 진행 중 게임은 응답에서도 closed=false — 고르기와 응답을 같은 시각으로 판정 (closed=true·resultUntil=null 조합 없음)")
    void openAtEndAtBoundary() {
        LocalDateTime endAt = TODAY.atTime(23, 59, 59);
        givenOpen(dayGame(TODAY));

        BalanceGameResponse response = balanceGameService.getCurrent(endAt);

        assertThat(response.closed()).isFalse();
        assertThat(response.serverTime()).isEqualTo(endAt);
    }

    @Test
    @DisplayName("버퍼(00:10): 진행 중 게임이 없으면 어제 게임 결과 — closed=true, resultUntil = 다음 게임 시작 00:30")
    void bufferShowsYesterdayResult() {
        LocalDateTime now = TODAY.atTime(0, 10);
        givenClosed(dayGame(TODAY.minusDays(1)), dayGame(TODAY));

        BalanceGameResponse response = balanceGameService.getCurrent(now);

        assertThat(response.optionA().text()).isEqualTo("2026-11-10 A");
        assertThat(response.closed()).isTrue();
        assertThat(response.resultUntil()).isEqualTo(TODAY.atTime(0, 30));
        assertThat(response.serverTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("다음 게임이 없으면 결과는 마감 + 30분(00:30:00)까지 — 00:29:59 는 결과, 00:30 부터 null")
    void noNextGame() {
        givenClosed(dayGame(TODAY.minusDays(1)), null);

        assertThat(balanceGameService.getCurrent(TODAY.atTime(0, 29, 59)).resultUntil()).isEqualTo(TODAY.atTime(0, 30));
        assertThat(balanceGameService.getCurrent(TODAY.atTime(0, 30))).isNull();
    }

    @Test
    @DisplayName("오늘 게임을 01:00 시작으로 넣으면 00:30~01:00 에도 어제 결과 (빈틈 없음), resultUntil = 01:00")
    void untilNextGameStart() {
        givenClosed(dayGame(TODAY.minusDays(1)), game(TODAY, TODAY.atTime(1, 0)));

        BalanceGameResponse response = balanceGameService.getCurrent(TODAY.atTime(0, 45));

        assertThat(response.optionA().text()).isEqualTo("2026-11-10 A");
        assertThat(response.resultUntil()).isEqualTo(TODAY.atTime(1, 0));
    }

    @Test
    @DisplayName("다음 게임이 며칠 뒤면 이어지는 게임으로 보지 않고 마감 + 30분까지만 결과 (지난 결과를 며칠 붙잡지 않음)")
    void farNextGame() {
        givenClosed(dayGame(TODAY.minusDays(1)), dayGame(TODAY.plusDays(3)));

        assertThat(balanceGameService.getCurrent(TODAY.atTime(0, 10)).resultUntil()).isEqualTo(TODAY.atTime(0, 30));
        assertThat(balanceGameService.getCurrent(TODAY.atTime(0, 31))).isNull();
    }

    @Test
    @DisplayName("진행 중·마감 게임이 모두 없으면 null")
    void noGame() {
        assertThat(balanceGameService.getCurrent(TODAY.atTime(12, 0))).isNull();
    }

    @Test
    @DisplayName("진행 중 게임이 있으면 마감 게임은 보지 않는다")
    void openWins() {
        givenOpen(dayGame(TODAY));

        balanceGameService.getCurrent(TODAY.atTime(0, 30));

        verify(balanceGameRepository, never()).findClosed(any(), any());
        verify(balanceGameRepository).findOpen(eq(TODAY.atTime(0, 30)), any());
    }
}
