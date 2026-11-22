package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 다시 올리지 않을 주소 — 운영자가 "거절" 처리 시 기록해 크롤러가 같은 후보를 재등록하지 않게 함
 */
@Entity
@Table(name = "status_link_blocked",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_status_link_blocked",
                        columnNames = {"competition_id", "link_type", "url", "page_title"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusLinkBlocked {

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

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "page_title", length = 500)
    private String pageTitle;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by")
    private Account blockedBy;

    public StatusLinkBlocked(Competition competition, CompetitionLinkType linkType,
                              String url, String pageTitle, Account blockedBy) {
        this.competition = competition;
        this.linkType = linkType;
        this.url = url;
        this.pageTitle = pageTitle;
        this.blockedBy = blockedBy;
        this.blockedAt = LocalDateTime.now();
    }
}
