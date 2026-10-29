package com.jiujitsu.api.domain.community.profile.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 대회 출전 부문 - 부문마다 메달 하나
 */
@Builder
@Entity
@Table(name = "community_profile_competition_divisions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionDivision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회원 삭제(UserCleanupService) 시 DB에서 프로필 → 대회 → 부문 순으로 함께 지워지도록 ON DELETE CASCADE
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_record_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CompetitionRecord competitionRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "competition_rank", nullable = false)
    private CompetitionRank competitionRank;  // 수상정보

    @Enumerated(EnumType.STRING)
    @Column(name = "competition_uniform")
    private CompetitionUniform competitionUniform; // 도복/노기

    @Enumerated(EnumType.STRING)
    @Column(name = "competition_belt")
    private BeltRank competitionBelt;  // 출전 띠

    @Column(name = "weight_limit")
    private Double competitionWeightLimit; // 체급 kg (소수 1자리)

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_bound")
    private CompetitionWeightBound competitionWeightBound; // 체급 이하/초과

    @Column(name = "is_absolute", nullable = false)
    private boolean competitionAbsolute; // 앱솔루트 여부 (체급과 함께 오지 않음)
}
