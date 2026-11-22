package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

/**
 * 사이트별 수집 설정 — 운영자가 크롤러 on/off 제어
 */
@Entity
@Table(name = "crawl_site_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlSiteSetting {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CrawlSite site;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "disabled_reason", length = 30)
    private CrawlSiteDisabledReason disabledReason;

    @Column(name = "disabled_memo", columnDefinition = "TEXT")
    private String disabledMemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disabled_by")
    private Account disabledBy;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enabled_by")
    private Account enabledBy;

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    public CrawlSiteSetting(CrawlSite site) {
        this.site = site;
        this.enabled = true;
    }

    public void disable(CrawlSiteDisabledReason reason, String memo, Account operator, LocalDateTime now) {
        this.enabled = false;
        this.disabledReason = reason;
        this.disabledMemo = memo;
        this.disabledBy = operator;
        this.disabledAt = now;
    }

    public void enable(Account operator, LocalDateTime now) {
        this.enabled = true;
        this.disabledReason = null;
        this.disabledMemo = null;
        this.enabledBy = operator;
        this.enabledAt = now;
    }
}
