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
 * 알림 전 확인 — 내일 마감인 칸을 오늘 크롤러가 확인하지 못한 경우 생성.
 * 운영자가 직접 확인 후 체크.
 */
@Entity
@Table(name = "notification_pre_checks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_pre_checks",
                        columnNames = {"competition_id", "field_name", "deadline_date"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    @ColumnDefault("'PENDING'")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPreCheckStatus status;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by")
    private Account checkedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private DeadlineNotification notification;

    public NotificationPreCheck(Competition competition, String fieldName,
                                  LocalDate deadlineDate, DeadlineNotification notification) {
        this.competition = competition;
        this.fieldName = fieldName;
        this.deadlineDate = deadlineDate;
        this.notification = notification;
        this.status = NotificationPreCheckStatus.PENDING;
    }

    public void check(Account operator) {
        this.status = NotificationPreCheckStatus.CHECKED;
        this.checkedAt = LocalDateTime.now();
        this.checkedBy = operator;
    }
}
