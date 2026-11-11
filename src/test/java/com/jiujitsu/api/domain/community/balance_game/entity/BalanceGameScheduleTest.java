package com.jiujitsu.api.domain.community.balance_game.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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
}
