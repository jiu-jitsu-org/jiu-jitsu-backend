package com.jiujitsu.api.domain.notice.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
