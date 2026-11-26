package com.jiujitsu.api.domain.boot_strap.repository;

import com.jiujitsu.api.domain.boot_strap.entity.AppNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AppNoticeRepository extends JpaRepository<AppNotice, Long> {

    @Query("""
        SELECT n FROM AppNotice n
        WHERE n.active = true
          AND n.startAt <= :now
          AND n.endAt >= :now
        ORDER BY n.startAt DESC
        LIMIT 1
    """)
    Optional<AppNotice> findCurrentNotice(LocalDateTime now);
}
