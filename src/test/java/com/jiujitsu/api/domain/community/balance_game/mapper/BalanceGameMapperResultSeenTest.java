package com.jiujitsu.api.domain.community.balance_game.mapper;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// resultSeen 은 내가 투표한 끝난 판에만 true/false (#290)
class BalanceGameMapperResultSeenTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);
    private static final LocalDateTime END_AT = DAY.atTime(23, 59, 59);

    private final BalanceGameMapper mapper = new BalanceGameMapper();

    private final BalanceGame game = BalanceGame.builder()
            .content(Content.builder().id(1L).contentType(ContentType.BALANCE).build())
            .optionAText("A").optionBText("B")
            .startAt(BalanceGame.defaultStartAt(DAY))
            .endAt(END_AT)
            .gameDate(DAY)
            .build();

    private Boolean resultSeen(BalanceGameOption myVote, boolean seen, LocalDateTime now) {
        return mapper.toResponse(game, 0, 0, false, false, null, 1, 0, myVote, seen, now, null).resultSeen();
    }

    @Test
    @DisplayName("투표한 끝난 판은 기록대로 true/false")
    void votedAndClosed() {
        LocalDateTime closed = END_AT.plusSeconds(1);

        assertThat(resultSeen(BalanceGameOption.A, true, closed)).isTrue();
        assertThat(resultSeen(BalanceGameOption.B, false, closed)).isFalse();
    }

    @Test
    @DisplayName("투표했어도 마감 시각까지는 null")
    void votedButOpen() {
        assertThat(resultSeen(BalanceGameOption.A, false, DAY.atTime(12, 0))).isNull();
        assertThat(resultSeen(BalanceGameOption.A, true, END_AT)).isNull();
    }

    @Test
    @DisplayName("투표하지 않은 판(비로그인 포함)은 끝났어도 null")
    void notVoted() {
        assertThat(resultSeen(null, false, END_AT.plusDays(1))).isNull();
    }
}
