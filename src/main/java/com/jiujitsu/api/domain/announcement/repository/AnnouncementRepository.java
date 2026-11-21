package com.jiujitsu.api.domain.announcement.repository;

import com.jiujitsu.api.domain.announcement.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // 보이는 공지 = 게시 시각이 있고 now 이전(같으면 포함). 정렬은 쿼리에 고정 — pageable 의 sort 는 쓰지 않는다
    @Query(value = "SELECT a FROM Announcement a"
            + " WHERE a.publishedAt IS NOT NULL AND a.publishedAt <= :now"
            + " ORDER BY a.publishedAt DESC, a.id DESC",
            countQuery = "SELECT COUNT(a) FROM Announcement a"
                    + " WHERE a.publishedAt IS NOT NULL AND a.publishedAt <= :now")
    Page<Announcement> findPublished(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT a FROM Announcement a"
            + " WHERE a.id = :id AND a.publishedAt IS NOT NULL AND a.publishedAt <= :now")
    Optional<Announcement> findPublishedById(@Param("id") Long id, @Param("now") LocalDateTime now);
}
