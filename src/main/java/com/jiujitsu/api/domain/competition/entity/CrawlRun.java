package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사이트별 수집 실행 기록 — 크롤러가 생성·관리, BE는 failure_reported_at/by 만 UPDATE
 */
@Entity
@Table(name = "crawl_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlSite site;

    @Column(name = "run_at")
    private LocalDateTime runAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CrawlRunStatus status;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @Column(name = "new_competitions_count")
    private Integer newCompetitionsCount;

    @Column(name = "changed_values_count")
    private Integer changedValuesCount;

    @Column(name = "disappeared_count")
    private Integer disappearedCount;

    @Column(name = "new_links_count")
    private Integer newLinksCount;

    @Column(name = "changed_links_count")
    private Integer changedLinksCount;

    // 담당자 전달 완료 시각 (우리가 UPDATE)
    @Column(name = "failure_reported_at")
    private LocalDateTime failureReportedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failure_reported_by")
    private Account failureReportedBy;

    public void reportFailure(Account operator) {
        this.failureReportedAt = LocalDateTime.now();
        this.failureReportedBy = operator;
    }
}
