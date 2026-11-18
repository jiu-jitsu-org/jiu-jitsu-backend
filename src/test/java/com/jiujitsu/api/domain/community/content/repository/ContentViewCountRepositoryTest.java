package com.jiujitsu.api.domain.community.content.repository;

import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// 조회수는 '봤음' 요청에서 DB 로 +1 (#249) — 동시에 열어도 덜 세지 않고, 수정 시각이 바뀌지 않는다
@DataJpaTest
class ContentViewCountRepositoryTest {

    @Autowired private ContentRepository contentRepository;
    @Autowired private TestEntityManager em;

    private Long content() {
        Long id = em.persist(Content.builder().contentType(ContentType.BOARD).build()).getId();
        em.flush();
        em.clear();
        return id;
    }

    @Test
    @DisplayName("부를 때마다 +1, 올린 값을 다시 읽을 수 있음")
    void increment() {
        Long id = content();

        assertThat(contentRepository.incrementViewCount(id)).isEqualTo(1);
        assertThat(contentRepository.incrementViewCount(id)).isEqualTo(1);

        assertThat(contentRepository.findViewCountById(id)).isEqualTo(2);
        assertThat(contentRepository.incrementViewCount(-1L)).isZero();
    }

    @Test
    @DisplayName("작성자는 첫 1회만 — 두 번째부터는 0행, 남의 조회는 그대로 셈")
    void authorOnce() {
        Long id = content();

        assertThat(contentRepository.incrementViewCountForAuthor(id)).isEqualTo(1);
        assertThat(contentRepository.incrementViewCountForAuthor(id)).isZero();
        contentRepository.incrementViewCount(id);

        assertThat(contentRepository.findViewCountById(id)).isEqualTo(2);
        em.clear();
        assertThat(em.find(Content.class, id).isAuthorViewCounted()).isTrue();
    }

    @Test
    @DisplayName("조회수를 올려도 수정 시각(감사 필드)은 바뀌지 않음 — 엔티티를 고쳐 올리던 때와 다름")
    void doesNotTouchAuditFields() {
        Long id = content();
        LocalDateTime updatedAt = em.find(Content.class, id).getUpdatedAt();
        assertThat(updatedAt).isNotNull();   // 감사가 켜져 있어 비교가 의미 있음
        em.clear();

        contentRepository.incrementViewCount(id);
        em.clear();

        assertThat(em.find(Content.class, id).getUpdatedAt()).isEqualTo(updatedAt);
    }
}
