package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompetitionCursorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 12, 6);

    @Test
    @DisplayName("인코딩한 커서를 다시 풀면 같은 위치·방향")
    void roundTrip() {
        CompetitionCursor after = CompetitionCursor.after(DATE, 42);
        CompetitionCursor before = CompetitionCursor.before(DATE, 7);

        assertThat(CompetitionCursor.decode(after.encode())).isEqualTo(after);
        assertThat(CompetitionCursor.decode(before.encode())).isEqualTo(before);
        assertThat(after.encode()).doesNotContain("=", "+", "/");   // URL 쿼리에 그대로 넣을 수 있게
    }

    @Test
    @DisplayName("같은 경계의 반대쪽: AFTER(d,i) ↔ BEFORE(d,i+1), BEFORE(d,i) ↔ AFTER(d,i-1)")
    void opposite() {
        assertThat(CompetitionCursor.after(DATE, 5).opposite()).isEqualTo(CompetitionCursor.before(DATE, 6));
        assertThat(CompetitionCursor.before(DATE, 5).opposite()).isEqualTo(CompetitionCursor.after(DATE, 4));
        assertThat(CompetitionCursor.startOf(DATE)).isEqualTo(CompetitionCursor.after(DATE, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-base64!!", "", "AFTER|2026-12-06", "SIDEWAYS|2026-12-06|1", "AFTER|2026-13-01|1", "AFTER|2026-12-06|x", "AFTER|+999999999-12-31|0", "BEFORE|-0001-01-01|5"})
    @DisplayName("형식이 틀린 커서는 WRONG_PARAMETER")
    void invalid(String raw) {
        String value = raw.contains("|")
                ? Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8))
                : raw;

        assertThatThrownBy(() -> CompetitionCursor.decode(value))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
    }

    @Test
    @DisplayName("기준일이 4자리 연도 범위를 벗어나면 WRONG_PARAMETER (DB 날짜 범위 밖 값이 쿼리까지 가지 않게)")
    void startOf_outOfRange() {
        assertThatThrownBy(() -> CompetitionCursor.startOf(LocalDate.of(10000, 1, 1)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
    }
}
