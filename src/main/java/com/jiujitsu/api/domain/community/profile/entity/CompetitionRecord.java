package com.jiujitsu.api.domain.community.profile.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

/**
 * 대회 기록 - 대회 하나에 출전 부문 여러 개
 */
@Builder
@Entity
@Table(
        name = "community_profile_competition_records",
        indexes = @Index(name = "idx_competition_record_profile", columnList = "community_profile_id")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_profile_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommunityProfile communityProfile;

    @Column(name = "competition_year", nullable = false)
    private Integer competitionYear;    // 대회출전년도

    @Column(name = "competition_name", nullable = false, length = 100)
    private String competitionName; // 대회명 (없으면 빈 값 - 나만 보기)

    @Column(name = "competition_pinned", nullable = false)
    private boolean competitionPinned;  // 대표 기록 고정 여부

    @Column(name = "competition_display_order")
    private Integer competitionDisplayOrder;    // 회원이 정한 순서 (고정한 것끼리, 안 한 것끼리 각각 0이 맨 위)

    @Builder.Default
    @OneToMany(mappedBy = "competitionRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @BatchSize(size = 100)
    private List<CompetitionDivision> divisions = new ArrayList<>(); // 출전 부문

    /**
     * 부문 추가
     */
    public void addDivision(CompetitionDivision division) {
        this.divisions.add(division);
    }

    /**
     * 대회명이 있는 기록인지 - 대회명 없는 기록은 나만 보이고 메달 합계에서 빠진다
     */
    public boolean isNamed() {
        return !competitionName.isBlank();
    }
}
