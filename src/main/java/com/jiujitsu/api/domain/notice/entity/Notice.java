package com.jiujitsu.api.domain.notice.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 알림함 알림.
 * 좋아요 알림은 (받는 사람, 대상) 마다 카드 한 장(groupKey)으로 묶고, 기준 개수에 닿을 때만 다시 올린다 (#196)
 */
@Entity
@Table(name = "notice",
        uniqueConstraints = {
                @UniqueConstraint(name = Notice.GROUP_KEY_UNIQUE_CONSTRAINT, columnNames = {"user_id", "group_key"})
        })
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate  // 바뀐 컬럼만 UPDATE — 읽음 처리와 좋아요 푸시(기준·제목·시각)가 겹쳐도 서로 덮어쓰지 않게
public class Notice extends BaseEntity {

    public static final String GROUP_KEY_UNIQUE_CONSTRAINT = "uk_notice_user_group_key";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String title;   // 제목

    @Column
    private String body;    // 내용

    @Column
    @Enumerated(EnumType.STRING)
    private FcmPushType pushType;

    @Column
    @Enumerated(EnumType.STRING)
    private PushActionType pushActionType;

    @Column
    private String data;

    @Column
    private Long senderUserId;  // 알림 발신자 (차단 필터용). 좋아요 카드는 여러 명이 누른 묶음이라 비어 있다

    @Column
    private Long refId;         // 댓글/대댓글 ID (삭제 안내 구분용). 댓글 좋아요 카드는 좋아요 받은 댓글

    @Column
    private Boolean isRead;

    // 알림함 정렬·표시 시각. 보통 만든 시각이고, 좋아요 카드는 마지막으로 푸시를 보낸 시각
    @Column(name = "notified_at", nullable = false)
    private LocalDateTime notifiedAt;

    // 좋아요 카드 키 (예: CONTENTS_LIKE:12). 그 밖의 알림은 null
    @Column(name = "group_key")
    private String groupKey;

    // 좋아요 카드가 이미 푸시를 보낸 가장 큰 기준 개수 (0 = 아직 없음). 같은 기준으로 두 번 보내지 않는다
    @Column(name = "like_pushed_count")
    private Integer likePushedCount;

    @PrePersist
    void fillNotifiedAt() {
        if (notifiedAt == null) {
            notifiedAt = LocalDateTime.now();
        }
    }

    public void setRead() {
        this.isRead = true;
    }

    public boolean isLikeCard() {
        return groupKey != null;
    }

    // 아직 푸시가 한 번도 안 간 좋아요 카드 (첫 기준 전) — 알림함에 보이지 않는다
    public boolean isLikeCardBeforeFirstPush() {
        return isLikeCard() && (likePushedCount == null || likePushedCount == 0);
    }

    /**
     * 좋아요 카드 제목·푸시 제목. 개수는 지금 실제 좋아요 수
     */
    public static String likeTitle(FcmPushType pushType, long count) {
        return switch (pushType) {
            case CONTENTS_LIKE -> "내 게시글에 좋아요가 " + count + "개 달렸어요";
            case COMMENTS_LIKE -> "내 댓글에 좋아요가 " + count + "개 달렸어요";
            default -> throw new IllegalArgumentException("좋아요 알림이 아님: " + pushType);
        };
    }

    /**
     * 좋아요 카드가 새 기준에 닿아 푸시를 보낼 때: 안 읽음으로 알림함 맨 위에 올린다
     */
    public void pushLike(int threshold, String title, LocalDateTime now) {
        this.likePushedCount = threshold;
        this.title = title;
        this.notifiedAt = now;
        this.isRead = false;
    }
}
