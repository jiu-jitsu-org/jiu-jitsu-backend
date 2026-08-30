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

    private BalanceGame saveGame(String aText, String bText, LocalDateTime endAt) {
        Content content = Content.builder()
                .contentType(ContentType.BALANCE)
                .build();
        BalanceGame game = BalanceGame.builder()
                .content(content)
                .optionAText(aText)
                .optionBText(bText)
                .endAt(endAt)
                .build();
        return balanceGameRepository.save(game);
    }

    @Test
    @DisplayName("마감 전 게임 중 가장 먼저 마감되는(=현재 진행중) 1건을 반환한다")
    void findCurrent() {
        LocalDateTime now = LocalDateTime.now();
        saveGame("A-과거", "B-과거", now.minusHours(1));            // 이미 마감
        BalanceGame soon = saveGame("A-곧마감", "B-곧마감", now.plusHours(1));
        saveGame("A-나중", "B-나중", now.plusHours(5));

        Optional<BalanceGame> result = balanceGameRepository.findTopByEndAtAfterOrderByEndAtAsc(now);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(soon.getId());
        assertThat(result.get().getOptionAText()).isEqualTo("A-곧마감");
        assertThat(result.get().getContent().getContentType()).isEqualTo(ContentType.BALANCE);
    }

    @Test
    @DisplayName("진행중인 게임이 없으면 빈 Optional 을 반환한다")
    void findCurrent_empty() {
        LocalDateTime now = LocalDateTime.now();
        saveGame("A", "B", now.minusHours(1)); // 마감된 것만 존재

        Optional<BalanceGame> result = balanceGameRepository.findTopByEndAtAfterOrderByEndAtAsc(now);

        assertThat(result).isEmpty();
    }
}
