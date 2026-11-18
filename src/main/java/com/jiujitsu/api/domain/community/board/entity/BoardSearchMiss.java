package com.jiujitsu.api.domain.community.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결과가 0건이었던 검색어 (#266). 누가 찾았는지는 남기지 않는다 — 그래서 작성자를 기록하는 BaseEntity 를 쓰지 않는다
 */
@Entity
@Table(
        name = "board_search_miss",
        indexes = {
                @Index(name = "idx_board_search_miss_searched_at", columnList = "searched_at")   // 기간 집계
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardSearchMiss {

    public static final int KEYWORD_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = KEYWORD_MAX_LENGTH)
    private String keyword;     // 검색 단어를 소문자 · 공백 하나로 이은 것 (같은 검색끼리 묶임)

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;
}
