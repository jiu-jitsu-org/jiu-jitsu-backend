package com.jiujitsu.api.domain.community.board.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 검색어 → 검색 단어 · LIKE 패턴 (#265)
class BoardServiceSearchWordsTest {

    @Test
    @DisplayName("공백(탭 · 줄바꿈 · NBSP · 전각 포함)으로 나누고 소문자로, 같은 단어는 하나로")
    void split() {
        assertThat(BoardService.searchWords("  오픈매트   NoGi\t강남\n nogi\u3000도복\u00A0A2L "))
                .containsExactly("오픈매트", "nogi", "강남", "도복", "a2l");
    }

    @Test
    @DisplayName("LIKE 특수문자는 이스케이프")
    void escape() {
        assertThat(BoardService.toLikePatterns(BoardService.searchWords("50% e_c"))).containsExactly("%50\\%%", "%e\\_c%");
    }

    @Test
    @DisplayName("비었거나 공백만이면 단어 없음 (검색 결과 빈 페이지)")
    void blank() {
        assertThat(BoardService.searchWords(null)).isEmpty();
        assertThat(BoardService.searchWords("   ")).isEmpty();
    }

    @Test
    @DisplayName("단어는 10개까지")
    void limit() {
        assertThat(BoardService.searchWords("a b c d e f g h i j k l")).hasSize(10).endsWith("j");
    }
}
