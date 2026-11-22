package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대회 출처 — 대회 1건에 출처 최대 1건. 직접 입력 대회는 site = DIRECT
 */
@Entity
@Table(name = "competition_sources",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_competition_sources_competition", columnNames = "competition_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompetitionSourceSite site;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "page_title", length = 500)
    private String pageTitle;

    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;

    // 목록에서 마지막으로 본 날 (사라짐 감지에 씀)
    @Column(name = "last_seen_in_list_at")
    private LocalDate lastSeenInListAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_by")
    private Account linkedBy;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    public CompetitionSource(Competition competition, CompetitionSourceSite site, String sourceUrl) {
        this.competition = competition;
        this.site = site;
        this.sourceUrl = sourceUrl;
    }

    public void link(Account operator, LocalDateTime now, String pageTitle) {
        this.linkedBy = operator;
        this.linkedAt = now;
        this.pageTitle = pageTitle;
    }

    public void updateCrawlInfo(LocalDateTime crawledAt, LocalDate seenInListAt, String pageTitle) {
        this.lastCrawledAt = crawledAt;
        this.lastSeenInListAt = seenInListAt;
        this.pageTitle = pageTitle;
    }
}
