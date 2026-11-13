package com.jiujitsu.api.global.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PopularCompetitionPropertiesTest {

    @Test
    @DisplayName("top·min-interests 는 1 이상이어야 한다 (잘못 넣으면 기동 실패)")
    void validates() {
        assertThatCode(() -> new PopularCompetitionProperties(3, 5)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new PopularCompetitionProperties(0, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PopularCompetitionProperties(3, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
