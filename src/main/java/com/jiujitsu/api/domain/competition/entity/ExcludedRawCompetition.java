package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대회 아님 처리 — 운영자가 크롤러가 발견한 항목을 "대회 아님"으로 제외 처리
 */
@Entity
@Table(name = "excluded_raw_competitions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_excluded_raw_competitions_site_url",
                        columnNames = {"site", "source_url"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExcludedRawCompetition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlSite site;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "page_title", length = 500)
    private String pageTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "excluded_reason", length = 30)
    private ExcludedReason excludedReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "excluded_by")
    private Account excludedBy;

    @Column(name = "excluded_at")
    private LocalDateTime excludedAt;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restored_by")
    private Account restoredBy;

    public ExcludedRawCompetition(CrawlSite site, String sourceUrl, String pageTitle,
                                   ExcludedReason reason, Account excludedBy) {
        this.site = site;
        this.sourceUrl = sourceUrl;
        this.pageTitle = pageTitle;
        this.excludedReason = reason;
        this.excludedBy = excludedBy;
        this.excludedAt = LocalDateTime.now();
    }

    public void restore(Account operator) {
        this.restoredBy = operator;
        this.restoredAt = LocalDateTime.now();
    }
}
