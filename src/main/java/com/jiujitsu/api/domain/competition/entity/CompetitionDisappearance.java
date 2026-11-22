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
 * 원문에서 사라짐 — 크롤러가 감지, 운영자가 처리(계속 공개 / 숨김)
 */
@Entity
@Table(name = "competition_disappearances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionDisappearance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    // 어느 출처에서 사라졌는지 (null = 출처 삭제됨)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private CompetitionSource source;

    @Column(name = "disappeared_at", nullable = false)
    private LocalDate disappearedAt;

    @Column(name = "last_seen_at")
    private LocalDate lastSeenAt;

    @ColumnDefault("'PENDING'")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisappearanceStatus status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Account resolvedBy;

    public CompetitionDisappearance(Competition competition, CompetitionSource source,
                                     LocalDate disappearedAt, LocalDate lastSeenAt) {
        this.competition = competition;
        this.source = source;
        this.disappearedAt = disappearedAt;
        this.lastSeenAt = lastSeenAt;
        this.status = DisappearanceStatus.PENDING;
    }

    public void resolve(DisappearanceStatus resolution, Account operator) {
        this.status = resolution;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = operator;
    }
}
