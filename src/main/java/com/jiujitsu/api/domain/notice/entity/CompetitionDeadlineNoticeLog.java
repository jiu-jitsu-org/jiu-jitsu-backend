package com.jiujitsu.api.domain.notice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대회 마감 전날 알림 발송 기록 — 같은 유저·대회·마감 종류·마감일에 한 번만 보내게 하는 멱등 키
 * 마감일이 키에 있어 날짜가 바뀌면(앞당김·미룸) 새 날짜 전날 한 번 더 보낸다
 * 알림(notice)은 30일 뒤 지워지지만 이 기록은 남는다. 대회·유저가 지워져도 남는 기록이라 FK 를 두지 않는다
 */
@Entity
@Table(name = "competition_deadline_notice_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_competition_deadline_notice_log",
                        columnNames = {"competition_id", "deadline_type", "deadline", "user_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CompetitionDeadlineNoticeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "deadline_type", nullable = false, length = 20)
    private CompetitionDeadlineType deadlineType;

    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public CompetitionDeadlineNoticeLog(Long competitionId, CompetitionDeadlineType deadlineType, LocalDate deadline, Long userId) {
        this.competitionId = competitionId;
        this.deadlineType = deadlineType;
        this.deadline = deadline;
        this.userId = userId;
    }
}
