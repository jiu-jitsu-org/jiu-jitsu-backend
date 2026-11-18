package com.jiujitsu.api.domain.community.board.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 검색어 → 검색 단어 (#265)
class BoardServiceSearchWordsTest {

    @Test
    @DisplayName("띄어쓰기 · 탭 · 줄바꿈으로 나누고 소문자로, 같은 단어는 하나로")
    void split() {
        assertThat(BoardService.searchWords("  오픈매트   NoGi\t강남\n nogi ")).containsExactly("오픈매트", "nogi", "강남");
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
