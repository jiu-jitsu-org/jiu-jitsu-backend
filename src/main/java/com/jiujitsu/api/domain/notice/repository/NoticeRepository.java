package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
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

    // 글 · 밸런스 게임 상세를 열면 그 콘텐츠에 대한 내 안 읽은 알림(댓글 · 답글 · 좋아요)을 읽음으로 (#246)
    // data 가 콘텐츠 id 인 알림만 — 대회 알림은 data 가 대회 id 라 actionTypes 로 뺀다
    @Modifying
    @Query("UPDATE Notice n SET n.isRead = true WHERE n.userId = :userId AND n.data = :data"
            + " AND n.pushActionType IN :actionTypes AND n.isRead = false")
    int markReadByUserIdAndContent(@Param("userId") Long userId, @Param("data") String data,
                                   @Param("actionTypes") Collection<PushActionType> actionTypes);

    // 보관 기간 지난 알림 삭제. 푸시가 간 좋아요 카드는 이미 보낸 기준을 들고 있어 지우지 않는다 (알림함에서만 숨김, NoticeService)
    // 푸시가 한 번도 안 간 좋아요 카드(like_pushed_count 0)는 잃을 기준이 없어 같이 지운다
    @Modifying
    @Query("DELETE FROM Notice n WHERE n.notifiedAt < :threshold AND (n.groupKey IS NULL OR n.likePushedCount = 0)")
    void deleteExpiredKeepingPushedLikeCards(@Param("threshold") LocalDateTime threshold);
}
