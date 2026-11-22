package com.jiujitsu.api.domain.competition.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

/**
 * 참가비 단계 (별도 엔티티) — 기존 @ElementCollection CompetitionFee 를 승격.
 * 테이블: competition_fee_stages (competition_fees 에서 rename + id 추가)
 *
 * 마이그레이션 필요:
 * 1. competition_fees 테이블에 id PK 추가
 * 2. 테이블 rename: competition_fees → competition_fee_stages
 * 3. 컬럼 rename: label → name, until → deadline, fee_order → sort_order
 * 4. Competition.fees @ElementCollection → @OneToMany(CompetitionFeeStage) 교체
 */
@Entity
@Table(name = "competition_fee_stages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionFeeStage {

    public static final int NAME_MAX_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    // 단계 이름 (예: "얼리버드", "기본 참가비")
    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    // 금액 (원, 0 = 무료)
    @Column(nullable = false)
    private int amount;

    // 이 금액의 마지막 날(당일 포함). 할인 단계에만 둔다 — null 이면 기본 참가비
    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public CompetitionFeeStage(Competition competition, String name, int amount,
                                LocalDate deadline, int sortOrder) {
        this.competition = competition;
        this.name = name;
        this.amount = amount;
        this.deadline = deadline;
        this.sortOrder = sortOrder;
    }
}
