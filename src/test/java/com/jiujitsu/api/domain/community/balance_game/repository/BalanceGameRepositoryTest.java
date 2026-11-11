package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BalanceGameRepositoryTest {

    @Autowired
    private BalanceGameRepository balanceGameRepository;

    private BalanceGame saveGame(String aText, LocalDateTime startAt, LocalDateTime endAt) {
        Content content = Content.builder()
                .contentType(ContentType.BALANCE)
                .build();
        BalanceGame game = BalanceGame.builder()
                .content(content)
                .optionAText(aText)
                .optionBText("B")
                .startAt(startAt)
                .endAt(endAt)
                .gameDate(endAt.toLocalDate())
                .build();
        return balanceGameRepository.save(game);
    }

    @Test
    @DisplayName("진행 중: 시작했고 마감 전인 게임 중 가장 먼저 마감되는 1건 (시작 전 게임은 빠짐)")
    void findOpen() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 11, 12, 0);
        saveGame("A-과거", now.minusDays(1), now.minusHours(1));            // 이미 마감
        BalanceGame soon = saveGame("A-곧마감", now.minusHours(10), now.plusHours(1));
        saveGame("A-나중", now.minusHours(10), now.plusHours(5));
        saveGame("A-시작전", now.plusHours(2), now.plusMinutes(30));       // 더 먼저 마감되지만 시작 전

        Optional<BalanceGame> result = balanceGameRepository.findOpen(now).stream().findFirst();

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(soon.getId());
        assertThat(result.get().getContent().getContentType()).isEqualTo(ContentType.BALANCE);
    }

    @Test
    @DisplayName("진행 중: 시작 시각과 같으면 진행 중, 진행 중인 게임이 없으면 빈 Optional")
    void findOpen_boundaryAndEmpty() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 11, 0, 30);
        BalanceGame startsNow = saveGame("A-지금시작", now, now.plusHours(23));

        assertThat(balanceGameRepository.findOpen(now).stream().findFirst())
                .map(BalanceGame::getId).contains(startsNow.getId());
        assertThat(balanceGameRepository.findOpen(now.minusSeconds(1)).stream().findFirst())
                .isEmpty();
    }

    @Test
    @DisplayName("최근 마감 순 / 시작 전 다음 게임: 마감 게임은 최근 것부터, 시작 전 게임은 먼저 시작하는 것부터")
    void findClosedAndUpcoming() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 11, 0, 10);
        saveGame("A-그제", now.minusDays(2), now.minusDays(1));
        BalanceGame yesterday = saveGame("A-어제", now.minusDays(1), now.minusMinutes(11));
        BalanceGame today = saveGame("A-오늘", now.plusMinutes(20), now.plusHours(23));
        saveGame("A-내일", now.plusDays(1), now.plusDays(2));

        assertThat(balanceGameRepository.findClosed(now, PageRequest.of(0, 1)))
                .extracting(BalanceGame::getId).containsExactly(yesterday.getId());
        assertThat(balanceGameRepository.findUpcoming(now, PageRequest.of(0, 1)))
                .extracting(BalanceGame::getId).containsExactly(today.getId());
    }
}
