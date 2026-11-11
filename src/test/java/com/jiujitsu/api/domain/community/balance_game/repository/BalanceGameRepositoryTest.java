package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

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

        Optional<BalanceGame> result = balanceGameRepository.findTopByStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtAsc(now, now);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(soon.getId());
        assertThat(result.get().getContent().getContentType()).isEqualTo(ContentType.BALANCE);
    }

    @Test
    @DisplayName("진행 중: 시작 시각과 같으면 진행 중, 진행 중인 게임이 없으면 빈 Optional")
    void findOpen_boundaryAndEmpty() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 11, 0, 30);
        BalanceGame startsNow = saveGame("A-지금시작", now, now.plusHours(23));

        assertThat(balanceGameRepository.findTopByStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtAsc(now, now))
                .map(BalanceGame::getId).contains(startsNow.getId());
        assertThat(balanceGameRepository.findTopByStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtAsc(now.minusSeconds(1), now.minusSeconds(1)))
                .isEmpty();
    }

    @Test
    @DisplayName("최근 마감: 기준 시각 뒤에 마감된 게임 중 가장 최근 1건, 아직 마감 전 게임은 빠짐")
    void findRecentlyClosed() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 11, 0, 10);
        saveGame("A-그제", now.minusDays(2), now.minusDays(1));
        BalanceGame yesterday = saveGame("A-어제", now.minusDays(1), now.minusMinutes(11));
        saveGame("A-오늘", now.plusMinutes(20), now.plusHours(23));

        assertThat(balanceGameRepository.findTopByEndAtAfterAndEndAtBeforeOrderByEndAtDesc(now.minusMinutes(30), now))
                .map(BalanceGame::getId).contains(yesterday.getId());
        assertThat(balanceGameRepository.findTopByEndAtAfterAndEndAtBeforeOrderByEndAtDesc(now.minusMinutes(5), now))
                .isEmpty();
    }
}
