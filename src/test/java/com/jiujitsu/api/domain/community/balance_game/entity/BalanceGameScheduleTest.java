package com.jiujitsu.api.domain.community.balance_game.entity;

import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceGameScheduleTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 11);

    private final BalanceGame game = BalanceGame.builder()
            .startAt(BalanceGame.defaultStartAt(DAY))
            .endAt(DAY.atTime(23, 59, 59))
            .gameDate(DAY)
            .build();

    @Test
    @DisplayName("기본 시작은 진행일 00:30 (00:00~00:30 은 전날 결과 버퍼)")
    void defaultStartAt() {
        assertThat(BalanceGame.defaultStartAt(DAY)).isEqualTo(DAY.atTime(0, 30));
    }

    @Test
    @DisplayName("시작 전 → 진행 중 → 마감: 00:29:59 시작 전, 00:30 시작, 23:59:59 까지 진행, 그 뒤 마감")
    void boundaries() {
        assertThat(game.isStarted(DAY.atTime(0, 29, 59))).isFalse();
        assertThat(game.isStarted(DAY.atTime(0, 30))).isTrue();
        assertThat(game.isClosed(DAY.atTime(23, 59, 59))).isFalse();
        assertThat(game.isClosed(DAY.plusDays(1).atStartOfDay())).isTrue();
    }

    @Test
    @DisplayName("투표 가능 판정: 시작 전 C0011, 마감 뒤 C0007, 진행 중 통과")
    void validateVotable() {
        assertThatThrownBy(() -> game.validateVotable(DAY.atTime(0, 10)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BALANCE_GAME_NOT_STARTED);
        assertThatThrownBy(() -> game.validateVotable(DAY.plusDays(1).atTime(0, 10)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BALANCE_GAME_CLOSED);
        assertThatCode(() -> game.validateVotable(DAY.atTime(12, 0))).doesNotThrowAnyException();
    }

    private BalanceGame next(LocalDateTime startAt) {
        return BalanceGame.builder().startAt(startAt).endAt(startAt.plusHours(20)).gameDate(startAt.toLocalDate()).build();
    }

    @Test
    @DisplayName("결과 노출 끝: 이어지는 다음 게임(하루 안 시작)이 있으면 그 시작, 없거나 며칠 뒤면 마감 + 30분(00:30:00)")
    void resultUntil() {
        LocalDate nextDay = DAY.plusDays(1);

        assertThat(game.resultUntil(next(nextDay.atTime(0, 30)))).isEqualTo(nextDay.atTime(0, 30));
        assertThat(game.resultUntil(next(nextDay.atTime(1, 0)))).isEqualTo(nextDay.atTime(1, 0));
        assertThat(game.resultUntil(null)).isEqualTo(nextDay.atTime(0, 30));
        assertThat(game.resultUntil(next(DAY.plusDays(3).atTime(0, 30)))).isEqualTo(nextDay.atTime(0, 30));
    }

    @Test
    @DisplayName("다음 게임이 없을 때 마감을 직접 넣은 게임: 22:00:00 → 22:30:00 (1초 어긋나지 않음), 21:15:30 → 21:46:00 (분 단위 올림)")
    void resultUntilCustomEndAt() {
        BalanceGame endsAt22 = BalanceGame.builder()
                .startAt(DAY.atTime(0, 30)).endAt(DAY.atTime(22, 0)).gameDate(DAY).build();
        BalanceGame endsAtOddSecond = BalanceGame.builder()
                .startAt(DAY.atTime(0, 30)).endAt(DAY.atTime(21, 15, 30)).gameDate(DAY).build();

        assertThat(endsAt22.resultUntil(null)).isEqualTo(DAY.atTime(22, 30));
        assertThat(endsAtOddSecond.resultUntil(null)).isEqualTo(DAY.atTime(21, 46));
    }
}
