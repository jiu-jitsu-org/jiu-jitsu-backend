package com.jiujitsu.api.domain.community.entity;

import com.jiujitsu.api.domain.user.entity.User;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_community_profile_user", columnNames = {"user_id"})
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CommunityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeltRank beltRank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeltStripe beltStripe; // 1~4 그랄

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(precision = 5, scale = 1)
    private BigDecimal weightKg; // 소수점 1자리

    @Column(length = 100)
    private String academyName;

    private Integer competitionYear; // 대회정보(년도)

    @Column(length = 100)
    private String competitionName; // 대회정보(이름)

    @Column(length = 100)
    private String favoriteTechnique;

    @Column(length = 100)
    private String bestTechnique;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void update(CommunityProfile source) {
        this.beltRank = source.beltRank;
        this.beltStripe = source.beltStripe;
        this.gender = source.gender;
        this.weightKg = source.weightKg;
        this.academyName = source.academyName;
        this.competitionYear = source.competitionYear;
        this.competitionName = source.competitionName;
        this.favoriteTechnique = source.favoriteTechnique;
        this.bestTechnique = source.bestTechnique;
    }
}
