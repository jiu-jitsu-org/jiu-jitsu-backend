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
 * 지금 값 유지 기록 — 운영자가 "지금 값 유지" 선택 시 기록.
 * 같은 크롤러 값이 다시 오면 "바뀐 값" 할 일을 올리지 않음.
 */
@Entity
@Table(name = "crawl_held_values",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crawl_held_values_competition_field_value",
                        columnNames = {"competition_id", "field_name", "held_crawl_value"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlHeldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    // 무시할 크롤러 값
    @Column(name = "held_crawl_value", nullable = false, columnDefinition = "TEXT")
    private String heldCrawlValue;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Account createdBy;

    public CrawlHeldValue(Competition competition, String fieldName, String heldCrawlValue, Account createdBy) {
        this.competition = competition;
        this.fieldName = fieldName;
        this.heldCrawlValue = heldCrawlValue;
        this.createdAt = LocalDateTime.now();
        this.createdBy = createdBy;
    }
}
