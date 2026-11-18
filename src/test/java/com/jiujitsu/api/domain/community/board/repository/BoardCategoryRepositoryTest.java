package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

// 카테고리 목록 — 사용 중인 것만 보이는 순서대로 (#263)
@DataJpaTest
class BoardCategoryRepositoryTest {

    @Autowired
    private BoardCategoryRepository boardCategoryRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("사용 중인 카테고리만, 보이는 순서대로 (저장 순서와 무관)")
    void activeInDisplayOrder() {
        save("매트 위 수다", 6, true);
        save("도장 · 오픈매트", 1, true);
        save("안 쓰는 칸", 0, false);
        save("대회", 2, true);

        assertThat(boardCategoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc())
                .extracting(BoardCategory::getName)
                .containsExactly("도장 · 오픈매트", "대회", "매트 위 수다");
    }

    private void save(String name, int displayOrder, boolean active) {
        em.persist(BoardCategory.builder().name(name).description(name + " 설명").displayOrder(displayOrder).isActive(active).build());
    }
}
