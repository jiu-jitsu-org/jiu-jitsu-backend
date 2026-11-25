package com.jiujitsu.api.domain.community.balance_game.factory;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.file.repository.ImageFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BalanceGameFactoryTest {

    private final BalanceGameFactory factory = new BalanceGameFactory(mock(ImageFileRepository.class));

    @Test
    @DisplayName("타이틀과 두 선택지 문구가 각자 자리에 들어간다 (#289)")
    void createBalanceGame() {
        LocalDate day = LocalDate.of(2026, 11, 26);

        BalanceGame game = factory.createBalanceGame(factory.createContent(), "더 싫은 패배는?", "옵션A", null, "옵션B", null,
                BalanceGame.defaultStartAt(day), day.atTime(23, 59, 59), day);

        assertThat(game.getTitle()).isEqualTo("더 싫은 패배는?");
        assertThat(game.getOptionAText()).isEqualTo("옵션A");
        assertThat(game.getOptionBText()).isEqualTo("옵션B");
    }
}
