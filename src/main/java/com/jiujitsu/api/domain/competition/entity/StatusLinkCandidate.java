package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 새 주소 후보 — 크롤러가 기존 승인 링크와 다른 주소 발견 시 생성. 운영자가 수락/거절
 */
@Entity
@Table(name = "status_link_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusLinkCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private CompetitionLinkType linkType;

    // 현재 승인된 링크 (null = 첫 링크)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_link_id")
    private CompetitionLink currentLink;

    @Column(name = "candidate_url", nullable = false, length = 1000)
    private String candidateUrl;

    @Column(name = "candidate_title", length = 500)
    private String candidateTitle;

    @Column(name = "found_at")
    private LocalDateTime foundAt;

    @ColumnDefault("'PENDING'")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkCandidateStatus status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Account resolvedBy;

    public StatusLinkCandidate(Competition competition, CompetitionLinkType linkType,
                                CompetitionLink currentLink, String candidateUrl, String candidateTitle) {
        this.competition = competition;
        this.linkType = linkType;
        this.currentLink = currentLink;
        this.candidateUrl = candidateUrl;
        this.candidateTitle = candidateTitle;
        this.foundAt = LocalDateTime.now();
        this.status = LinkCandidateStatus.PENDING;
    }

    public void resolve(LinkCandidateStatus resolution, Account operator) {
        this.status = resolution;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = operator;
    }
}
