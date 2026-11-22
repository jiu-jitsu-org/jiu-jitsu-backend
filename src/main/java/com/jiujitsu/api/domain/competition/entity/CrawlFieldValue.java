package com.jiujitsu.api.domain.competition.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 칸별 크롤러 최신 값 — 외부 의존 테이블 (크롤러 담당자가 관리, BE 읽기 전용)
 */
@Entity
@Table(name = "crawl_field_values",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crawl_field_values_competition_field",
                        columnNames = {"competition_id", "field_name"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "crawled_value", columnDefinition = "TEXT")
    private String crawledValue;

    @Column(name = "raw_display", length = 255)
    private String rawDisplay;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private CompetitionSource source;
}
