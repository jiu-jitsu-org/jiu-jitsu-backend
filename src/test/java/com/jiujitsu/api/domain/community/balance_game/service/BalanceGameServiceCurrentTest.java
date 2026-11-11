package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// 메인 '오늘의 게임'이 00:00~00:30 버퍼 동안 어제 게임 결과를, 그 뒤 오늘 게임을 고르는지 실제 쿼리로 확인 (#195)
@DataJpaTest
@Import(BalanceGameService.class)
class BalanceGameServiceCurrentTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    @Autowired private BalanceGameService balanceGameService;
    @Autowired private TestEntityManager em;

    @MockBean private BalanceGameFactory balanceGameFactory;
    @MockBean private BalanceGameMapper balanceGameMapper;
    @MockBean private CommunityCommentsService communityCommentsService;
    @MockBean private ContentService contentService;
    @MockBean private NoticeService noticeService;
    @MockBean private AuthenticationFacade authenticationFacade;

    // 그날의 게임: 00:30 시작, 23:59:59 마감 (기본 일정)
    private BalanceGame dayGame(LocalDate day) {
        return game(day, BalanceGame.defaultStartAt(day));
    }

    private BalanceGame game(LocalDate day, LocalDateTime startAt) {
        return em.persist(BalanceGame.builder()
                .content(Content.builder().contentType(ContentType.BALANCE).build())
                .optionAText(day + " A")
                .optionBText(day + " B")
                .startAt(startAt)
                .endAt(day.atTime(23, 59, 59))
                .gameDate(day)
                .build());
    }

    private String current(LocalDateTime now) {
        return balanceGameService.findCurrent(now).map(current -> current.game().getOptionAText()).orElse(null);
    }

    private LocalDateTime resultUntil(LocalDateTime now) {
        return balanceGameService.findCurrent(now).map(BalanceGameService.CurrentGame::resultUntil).orElse(null);
    }

    @Test
    @DisplayName("00:00~00:30 은 어제 게임(결과), 00:30 부터 오늘 게임. 미리 등록한 내일 게임은 나오지 않는다")
    void buffer() {
        dayGame(TODAY.minusDays(1));
        dayGame(TODAY);
        dayGame(TODAY.plusDays(1));
        em.flush();

        assertThat(current(TODAY.minusDays(1).atTime(23, 59, 59))).isEqualTo("2026-11-10 A");
        assertThat(current(TODAY.atTime(0, 0))).isEqualTo("2026-11-10 A");
        assertThat(current(TODAY.atTime(0, 29, 59))).isEqualTo("2026-11-10 A");
        assertThat(current(TODAY.atTime(0, 30))).isEqualTo("2026-11-11 A");
        assertThat(current(TODAY.atTime(23, 59, 59))).isEqualTo("2026-11-11 A");
    }

    @Test
    @DisplayName("버퍼가 지나도 오늘 게임이 없으면 null (어제 결과를 계속 띄우지 않음), 어제 게임이 없으면 버퍼 동안도 null")
    void noGame() {
        dayGame(TODAY.minusDays(1));
        em.flush();

        assertThat(current(TODAY.atTime(0, 10))).isEqualTo("2026-11-10 A");
        assertThat(current(TODAY.atTime(0, 31))).isNull();
        assertThat(current(TODAY.plusDays(1).atTime(0, 10))).isNull();
    }

    @Test
    @DisplayName("결과는 다음 게임이 시작할 때까지 — 오늘 게임을 01:00 시작으로 넣으면 00:30~01:00 에도 어제 결과 (빈틈 없음), resultUntil = 01:00")
    void untilNextGameStart() {
        dayGame(TODAY.minusDays(1));
        game(TODAY, TODAY.atTime(1, 0));
        em.flush();

        assertThat(current(TODAY.atTime(0, 45))).isEqualTo("2026-11-10 A");
        assertThat(resultUntil(TODAY.atTime(0, 45))).isEqualTo(TODAY.atTime(1, 0));
        assertThat(current(TODAY.atTime(1, 0))).isEqualTo("2026-11-11 A");
        assertThat(resultUntil(TODAY.atTime(1, 0))).isNull();
    }

    @Test
    @DisplayName("기본 일정: 버퍼 동안 resultUntil = 다음 게임 시작 00:30, 다음 게임이 없으면 마감 + 30분(00:30:00)")
    void resultUntilDefault() {
        dayGame(TODAY.minusDays(1));
        em.flush();
        assertThat(resultUntil(TODAY.atTime(0, 10))).isEqualTo(TODAY.atTime(0, 30));

        dayGame(TODAY);
        em.flush();
        assertThat(resultUntil(TODAY.atTime(0, 10))).isEqualTo(TODAY.atTime(0, 30));
    }

    @Test
    @DisplayName("다음 게임이 며칠 뒤면 이어지는 게임으로 보지 않고 마감 + 30분까지만 결과 (지난 결과를 며칠 붙잡지 않음)")
    void farNextGame() {
        dayGame(TODAY.minusDays(1));
        dayGame(TODAY.plusDays(3));
        em.flush();

        assertThat(current(TODAY.atTime(0, 10))).isEqualTo("2026-11-10 A");
        assertThat(current(TODAY.atTime(0, 31))).isNull();
    }
}
