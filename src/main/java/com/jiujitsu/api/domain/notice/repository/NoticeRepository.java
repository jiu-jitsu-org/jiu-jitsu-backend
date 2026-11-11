package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.Notice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByUserId(Long userId);
    // 알림함 순서: 알림 시각(좋아요 카드는 마지막 푸시 시각) 최신순
    List<Notice> findByUserIdOrderByNotifiedAtDescIdDesc(Long userId);
    List<Notice> findByUserIdAndIsReadFalse(Long userId);
    long countByUserIdAndIsRead(Long userId, Boolean isRead);

    // 좋아요 카드 잠금 조회 — 동시에 눌린 좋아요가 같은 기준으로 두 번 푸시하지 않게
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Notice> findForUpdateByUserIdAndGroupKey(Long userId, String groupKey);

    @Modifying
    @Query("DELETE FROM Notice n WHERE n.notifiedAt < :threshold")
    void deleteByNotifiedAtBefore(@Param("threshold") LocalDateTime threshold);
}
