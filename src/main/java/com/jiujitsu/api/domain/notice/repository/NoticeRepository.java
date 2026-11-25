package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
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
import java.util.Set;

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

    // 한 콘텐츠에 대한 내 안 읽은 알림 중 이 종류만 읽음으로 (#291)
    @Modifying
    @Query("UPDATE Notice n SET n.isRead = true WHERE n.userId = :userId AND n.data = :data"
            + " AND n.pushType = :pushType AND n.isRead = false")
    int markReadByUserIdAndContentAndPushType(@Param("userId") Long userId, @Param("data") String data,
                                              @Param("pushType") FcmPushType pushType);

    // 보관 기간 지난 알림 삭제. 푸시가 간 좋아요 카드는 이미 보낸 기준을 들고 있어 지우지 않는다 (알림함에서만 숨김, NoticeService)
    // 푸시가 한 번도 안 간 좋아요 카드(like_pushed_count 0)는 잃을 기준이 없어 같이 지운다
    @Modifying
    @Query("DELETE FROM Notice n WHERE n.notifiedAt < :threshold AND (n.groupKey IS NULL OR n.likePushedCount = 0)")
    void deleteExpiredKeepingPushedLikeCards(@Param("threshold") LocalDateTime threshold);

    /**
     * 안 읽은 일반 알림(좋아요 카드 제외) 수 집계.
     * 좋아요 카드는 실제 좋아요 수 확인이 필요해 {@link #findUnreadLikeCards}로 별도 처리한다.
     *
     * <p>빈 컬렉션 대응: blockedUserIds·excludedDataStrings 가 비어 있을 때는
     * 절대 매칭되지 않는 더미 값(예: -1L, "-1")을 넣어 호출할 것.
     */
    @Query("""
            SELECT COUNT(n) FROM Notice n
            WHERE n.userId = :userId
              AND n.isRead = false
              AND n.groupKey IS NULL
              AND (n.pushActionType = :competitionDetail
                   OR ((n.senderUserId IS NULL OR n.senderUserId NOT IN :blockedUserIds)
                       AND (n.data IS NULL OR n.data NOT IN :excludedDataStrings)))
            """)
    long countUnreadFiltered(
            @Param("userId") Long userId,
            @Param("blockedUserIds") Collection<Long> blockedUserIds,
            @Param("excludedDataStrings") Collection<String> excludedDataStrings,
            @Param("competitionDetail") PushActionType competitionDetail
    );

    /**
     * 안 읽은 좋아요 카드 목록 — 실제 좋아요 수를 확인해 0개 카드를 걸러내기 위해 엔티티를 로드한다.
     * 사용자당 카드 수가 소수라 로드 비용은 미미하다.
     * 첫 푸시 전 카드(likePushedCount = 0)와 보관 기간(likeCardCutoff) 지난 카드는 SQL에서 제외한다.
     */
    @Query("""
            SELECT n FROM Notice n
            WHERE n.userId = :userId
              AND n.isRead = false
              AND n.groupKey IS NOT NULL
              AND n.likePushedCount IS NOT NULL AND n.likePushedCount > 0
              AND n.notifiedAt >= :likeCardCutoff
              AND (n.senderUserId IS NULL OR n.senderUserId NOT IN :blockedUserIds)
              AND (n.data IS NULL OR n.data NOT IN :excludedDataStrings)
            """)
    List<Notice> findUnreadLikeCards(
            @Param("userId") Long userId,
            @Param("blockedUserIds") Collection<Long> blockedUserIds,
            @Param("excludedDataStrings") Collection<String> excludedDataStrings,
            @Param("likeCardCutoff") LocalDateTime likeCardCutoff
    );
}
