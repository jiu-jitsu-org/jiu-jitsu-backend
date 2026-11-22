package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 변경 이력 — 운영자·크롤러·서버의 대회 데이터 변경 기록
 *
 * action 목록:
 * 운영자: field_save, crawl_value_apply, crawl_value_hold, verify, publish, unpublish, hide,
 *         link_approve, link_reject, link_hide, link_re_approve, link_check_ok, link_early_close,
 *         link_address_change, link_add, link_delete, report_process, report_cancel,
 *         source_link, source_unlink, competition_create
 * 크롤러·서버: link_check_raised, disappearance_detected, notification_sent,
 *              notification_skipped, notification_manual
 */
@Entity
@Table(name = "change_history",
        indexes = {
                @Index(name = "idx_change_history_competition_created", columnList = "competition_id, created_at DESC")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null = 대회 비특정 작업
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ChangeActorType actorType;

    // OPERATOR 만 사용. CRAWLER/SERVER 는 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Account actor;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChangeHistory(Competition competition, ChangeActorType actorType, Account actor,
                          String action, String fieldName, String oldValue, String newValue,
                          String reason, String memo) {
        this.competition = competition;
        this.actorType = actorType;
        this.actor = actor;
        this.action = action;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.memo = memo;
        this.createdAt = LocalDateTime.now();
    }
}
