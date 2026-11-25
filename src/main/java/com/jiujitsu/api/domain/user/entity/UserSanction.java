package com.jiujitsu.api.domain.user.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 유저 제재 이력. 경고·일시정지·영구 차단을 기록하고, 해제도 같은 행에 남긴다.
 */
@Entity
@Table(name = "user_sanctions",
        indexes = @Index(name = "idx_user_sanctions_user", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserSanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SanctionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SanctionReason reason;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    // 일시정지: startedAt + durationDays. 경고·영구차단: null
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private Account processedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 해제 정보 (일시정지·영구차단 수동 해제 시)
    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by")
    private Account releasedBy;

    @Column(name = "released_reason", length = 500)
    private String releasedReason;

    public UserSanction(User user, SanctionType type, SanctionReason reason, String reasonDetail,
                        LocalDateTime startedAt, LocalDateTime endedAt, Account processedBy) {
        this.user = user;
        this.type = type;
        this.reason = reason;
        this.reasonDetail = reasonDetail;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.processedBy = processedBy;
    }

    public boolean isActive() {
        if (releasedAt != null) return false;
        if (endedAt == null) return true; // WARNING or PERMANENT_BAN
        return endedAt.isAfter(LocalDateTime.now());
    }

    public void release(Account operator, String reason) {
        this.releasedAt = LocalDateTime.now();
        this.releasedBy = operator;
        this.releasedReason = reason;
    }
}
