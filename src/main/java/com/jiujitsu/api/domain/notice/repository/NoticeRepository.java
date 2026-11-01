package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByUserId(Long userId);
    List<Notice> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notice> findByUserIdAndIsReadFalse(Long userId);
    long countByUserIdAndIsRead(Long userId, Boolean isRead);

    @Modifying
    @Query("DELETE FROM Notice n WHERE n.createdAt < :threshold")
    void deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
