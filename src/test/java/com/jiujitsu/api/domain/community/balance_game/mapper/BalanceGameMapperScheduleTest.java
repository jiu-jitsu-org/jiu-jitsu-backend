package com.jiujitsu.api.domain.community.balance_game.mapper;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceGameMapperScheduleTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    private final BalanceGameMapper mapper = new BalanceGameMapper();

    private final BalanceGame game = BalanceGame.builder()
            .content(Content.builder().id(1L).contentType(ContentType.BALANCE).build())
            .optionAText("A").optionBText("B")
            .startAt(BalanceGame.defaultStartAt(DAY))
            .endAt(DAY.atTime(23, 59, 59))
            .gameDate(DAY)
            .build();

    private BalanceGameResponse at(LocalDateTime now) {
        return mapper.toResponse(game, 0, 0, false, false, null, 0, 0, null, now);
    }

    @Test
    @DisplayName("버퍼(다음 날 00:10) 동안 메인에 나오는 전날 게임은 started=true, closed=true (결과만, 투표 불가)")
    void bufferResponseIsClosed() {
        BalanceGameResponse response = at(DAY.plusDays(1).atTime(0, 10));

        assertThat(response.startAt()).isEqualTo(DAY.atTime(0, 30));
        assertThat(response.started()).isTrue();
        assertThat(response.closed()).isTrue();
    }

    @Test
    @DisplayName("시작 전(00:10)은 started=false, closed=false / 진행 중은 started=true, closed=false")
    void beforeStartAndOpen() {
        assertThat(at(DAY.atTime(0, 10))).extracting(BalanceGameResponse::started, BalanceGameResponse::closed)
                .containsExactly(false, false);
        assertThat(at(DAY.atTime(12, 0))).extracting(BalanceGameResponse::started, BalanceGameResponse::closed)
                .containsExactly(true, false);
    }
}
