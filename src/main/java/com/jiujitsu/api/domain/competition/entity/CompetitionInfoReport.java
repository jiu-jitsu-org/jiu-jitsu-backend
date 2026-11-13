package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
// 대회 정보 제보 ("정보가 달라요"). 게시글·댓글 신고(report)와 달리 제재 흐름이 없어 따로 둔다
// 하루 횟수 확인(대회·유저·항목·제보 시각)을 인덱스 하나로 처리한다. competition_id 선두라 대회 삭제 CASCADE 에도 쓰인다
@Table(name = "competition_info_reports",
        indexes = {
                @Index(name = "idx_competition_info_report_limit", columnList = "competition_id, user_id, field, created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
// 제보자는 도메인 값이라 BaseEntity 의 감사 필드가 아닌 user 로 둔다 (CompetitionInterest 와 같음)
// 대회·유저가 지워지면 제보도 함께 지운다
public class CompetitionInfoReport {

    public static final int DETAIL_MAX_LENGTH = 200;
    public static final int NOTE_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompetitionInfoField field;

    @Column(length = DETAIL_MAX_LENGTH)
    private String detail;  // 무엇이 맞는지 등 (OTHER 면 필수)

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 운영자 처리 (#222) — 처리한 시각(null = 미처리)과 메모(무엇을 했는지, 선택)
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note", length = NOTE_MAX_LENGTH)
    private String resolutionNote;

    /**
     * 내용은 앞뒤 공백을 지우고, 비면 null. "다른 정보"는 무엇이 다른지 알 수 없으므로 내용이 있어야 한다
     */
    public static CompetitionInfoReport of(Competition competition, User user, CompetitionInfoField field, String detail) {
        String cleaned = detail == null || detail.isBlank() ? null : detail.strip();
        if (field == null
                || (field == CompetitionInfoField.OTHER && cleaned == null)
                || (cleaned != null && cleaned.length() > DETAIL_MAX_LENGTH)) {
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }
        CompetitionInfoReport report = new CompetitionInfoReport();
        report.competition = competition;
        report.user = user;
        report.field = field;
        report.detail = cleaned;
        return report;
    }

    /**
     * 처리 완료 (#222). 처음 처리한 시각을 남기고, 다시 처리하면 메모만 바꾼다. 메모는 앞뒤 공백을 지우고 비면 null
     */
    public void resolve(LocalDateTime now, String note) {
        if (resolvedAt == null) {
            resolvedAt = now;
        }
        resolutionNote = note == null || note.isBlank() ? null : note.strip();
    }

    /**
     * 처리 되돌리기 — 시각 · 메모를 지운다
     */
    public void unresolve() {
        resolvedAt = null;
        resolutionNote = null;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }
}

