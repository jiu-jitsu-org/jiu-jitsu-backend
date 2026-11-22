package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

/**
 * 크롤러 신규 대회 후보 — 크롤러가 생성·관리, BE는 status/linked_competition_id/processed 만 UPDATE
 * 같은 원문 주소는 하나만 (uk_raw_competition_site_url)
 */
@Entity
@Table(name = "raw_competitions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_raw_competition_site_url",
                        columnNames = {"site", "source_url"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawCompetition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlSite site;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    // 회차가 포함된 페이지 제목 (스트릿 주짓수처럼 회차가 바뀌는 사이트 식별용)
    @Column(name = "page_title", length = 500)
    private String pageTitle;

    // 크롤러가 읽은 대회 정보 (텍스트 그대로)
    @Column(name = "crawled_name", length = 300)
    private String crawledName;

    @Column(name = "crawled_date", length = 100)
    private String crawledDate;

    @Column(name = "crawled_region", length = 100)
    private String crawledRegion;

    @Column(name = "crawled_place", length = 300)
    private String crawledPlace;

    @Column(name = "crawled_registration_deadline", length = 100)
    private String crawledRegistrationDeadline;

    @Column(name = "crawled_registration_site_url", length = 1000)
    private String crawledRegistrationSiteUrl;

    @Column(name = "found_at")
    private LocalDateTime foundAt;

    @ColumnDefault("'PENDING'")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawCompetitionStatus status;

    // 연결된 기존 대회 (LINKED/DRAFT_CREATED/PUBLISHED 시 채워짐)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_competition_id")
    private Competition linkedCompetition;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private Account processedBy;

    public void linkTo(Competition competition, Account operator) {
        this.linkedCompetition = competition;
        this.status = RawCompetitionStatus.LINKED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = operator;
    }

    public void markDraftCreated(Competition competition, Account operator) {
        this.linkedCompetition = competition;
        this.status = RawCompetitionStatus.DRAFT_CREATED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = operator;
    }

    public void markPublished(Account operator) {
        this.status = RawCompetitionStatus.PUBLISHED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = operator;
    }

    public void exclude(Account operator) {
        this.status = RawCompetitionStatus.EXCLUDED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = operator;
    }
}
