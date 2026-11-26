package com.jiujitsu.api.global.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceResultNoticePropertiesTest {

    @Test
    @DisplayName("min-votes 는 1 이상이어야 한다 (잘못 넣으면 기동 실패)")
    void validates() {
        assertThatCode(() -> new BalanceResultNoticeProperties(1)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new BalanceResultNoticeProperties(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
