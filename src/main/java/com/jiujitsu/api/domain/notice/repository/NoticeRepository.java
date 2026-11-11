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

    // 보관 기간 지난 알림 삭제. 푸시가 간 좋아요 카드는 이미 보낸 기준을 들고 있어 지우지 않는다 (알림함에서만 숨김, NoticeService)
    // 푸시가 한 번도 안 간 좋아요 카드(like_pushed_count 0)는 잃을 기준이 없어 같이 지운다
    @Modifying
    @Query("DELETE FROM Notice n WHERE n.notifiedAt < :threshold AND (n.groupKey IS NULL OR n.likePushedCount = 0)")
    void deleteExpiredExceptLikeCards(@Param("threshold") LocalDateTime threshold);
}
