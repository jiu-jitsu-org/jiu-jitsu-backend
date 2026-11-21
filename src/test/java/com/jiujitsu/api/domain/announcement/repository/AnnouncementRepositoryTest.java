package com.jiujitsu.api.domain.announcement.repository;

import com.jiujitsu.api.domain.announcement.entity.Announcement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AnnouncementRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 10, 0);

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private TestEntityManager em;

    private Announcement save(String title, LocalDateTime publishedAt) {
        return em.persistAndFlush(Announcement.create(title, "본문", publishedAt));
    }

    @Test
    @DisplayName("목록은 게시 시각 최신순, 같은 시각이면 id 큰 것 먼저")
    void findPublished_latestFirst() {
        Announcement old = save("지난 공지", NOW.minusDays(3));
        Announcement sameA = save("같은 시각 1", NOW.minusDays(1));
        Announcement sameB = save("같은 시각 2", NOW.minusDays(1));

        Page<Announcement> page = announcementRepository.findPublished(NOW, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Announcement::getId)
                .containsExactly(sameB.getId(), sameA.getId(), old.getId());
    }

    @Test
    @DisplayName("게시 시각이 없거나 미래인 공지는 빠지고, 지금과 같은 시각은 보인다")
    void findPublished_excludesUnpublished() {
        Announcement onTime = save("정각", NOW);
        save("임시", null);
        save("예약", NOW.plusSeconds(1));

        Page<Announcement> page = announcementRepository.findPublished(NOW, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Announcement::getId).containsExactly(onTime.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("보이는 공지가 없으면 빈 페이지")
    void findPublished_empty() {
        save("예약", NOW.plusDays(1));

        Page<Announcement> page = announcementRepository.findPublished(NOW, PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("페이지를 나눠도 개수와 순서가 맞다")
    void findPublished_paging() {
        for (int i = 0; i < 3; i++) {
            save("공지 " + i, NOW.minusHours(i));
        }

        Page<Announcement> second = announcementRepository.findPublished(NOW, PageRequest.of(1, 2));

        assertThat(second.getTotalElements()).isEqualTo(3);
        assertThat(second.getContent()).extracting(Announcement::getTitle).containsExactly("공지 2");
    }

    @Test
    @DisplayName("상세는 게시된 공지만 — 임시 · 예약은 없음")
    void findPublishedById() {
        Announcement published = save("게시", NOW.minusMinutes(1));
        Announcement draft = save("임시", null);
        Announcement scheduled = save("예약", NOW.plusMinutes(1));

        assertThat(announcementRepository.findPublishedById(published.getId(), NOW)).isPresent();
        assertThat(announcementRepository.findPublishedById(draft.getId(), NOW)).isEmpty();
        assertThat(announcementRepository.findPublishedById(scheduled.getId(), NOW)).isEmpty();
        assertThat(announcementRepository.findPublishedById(-1L, NOW)).isEmpty();
    }
}
