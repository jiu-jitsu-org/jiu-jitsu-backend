package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 대회 상태 링크 (#221) — 대회 상태에 맞는 접수 사이트 페이지 주소. 페이지 내용은 저장하지 않는다
 * 대회마다 종류별 하나. 운영자가 승인한 링크만, 보이는 기간(CompetitionLinkWindow) 안에서 앱에 나간다
 */
@Entity
@Table(name = "competition_links",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_competition_link_competition_type", columnNames = {"competition_id", "type"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionLink extends BaseEntity {

    public static final int URL_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대회가 지워지면 링크도 함께 지운다
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CompetitionLinkType type;

    @Column(name = "url", nullable = false, length = URL_MAX_LENGTH)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CompetitionLinkStatus status;

    // 처음 승인한 시각 — 참가자 현황 · 대진표는 이때부터 보인다. 숨겼다가 다시 켜도 유지, 주소가 바뀌면 지움
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public CompetitionLink(Competition competition, CompetitionLinkType type, String url) {
        this.competition = competition;
        this.type = type;
        this.url = url;
        this.status = CompetitionLinkStatus.PENDING;
    }

    /**
     * 주소 바꾸기. 바뀌면 승인 전으로 — 바뀐 링크는 운영자가 다시 확인하기 전엔 보이지 않는다. 같은 주소면 그대로
     */
    public void changeUrl(String url) {
        if (this.url.equals(url)) {
            return;
        }
        this.url = url;
        this.status = CompetitionLinkStatus.PENDING;
        this.approvedAt = null;
    }

    /**
     * 승인(켜기). 처음 승인한 시각만 남긴다
     */
    public void approve(LocalDateTime now) {
        this.status = CompetitionLinkStatus.APPROVED;
        if (this.approvedAt == null) {
            this.approvedAt = now;
        }
    }

    /**
     * 숨기기(끄기)
     */
    public void hide() {
        this.status = CompetitionLinkStatus.HIDDEN;
    }
}
