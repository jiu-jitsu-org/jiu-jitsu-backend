package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 마감 알림 발송 결과 — 발송 건 단위 어드민 추적용
 * (유저별 발송 dedup 로그인 competition_deadline_notice_logs 와 별도)
 */
@Entity
@Table(name = "deadline_notifications",
        indexes = {
                @Index(name = "idx_deadline_notifications_date_status", columnList = "deadline_date, status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeadlineNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    // 'registration_deadline' / 'early_bird_deadline' / 'fee_stage'
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    // 할인 단계 기한 알림 시 (fee_stage 알림에만 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_stage_id")
    private CompetitionFeeStage feeStage;

    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "recipient_count")
    private Integer recipientCount;

    @Column(name = "skipped_count")
    private Integer skippedCount;

    @ColumnDefault("'PENDING'")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeadlineNotificationStatus status;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    // null = 자동 발송
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by")
    private Account sentBy;

    public DeadlineNotification(Competition competition, String fieldName,
                                  CompetitionFeeStage feeStage, LocalDate deadlineDate,
                                  LocalDateTime scheduledAt) {
        this.competition = competition;
        this.fieldName = fieldName;
        this.feeStage = feeStage;
        this.deadlineDate = deadlineDate;
        this.scheduledAt = scheduledAt;
        this.status = DeadlineNotificationStatus.PENDING;
    }

    public void markSent(int recipientCount, int skippedCount, Account sentBy) {
        this.status = DeadlineNotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.recipientCount = recipientCount;
        this.skippedCount = skippedCount;
        this.sentBy = sentBy;
    }

    public void markFailed(String errorDetail) {
        this.status = DeadlineNotificationStatus.FAILED;
        this.errorDetail = errorDetail;
    }

    public void markSkipped() {
        this.status = DeadlineNotificationStatus.SKIPPED;
    }
}
