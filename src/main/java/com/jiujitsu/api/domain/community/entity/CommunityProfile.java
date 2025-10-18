package com.jiujitsu.api.domain.community.entity;

import com.jiujitsu.api.domain.community.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_community_profile_user", columnNames = {"user_id"}),
        @UniqueConstraint(name = "uk_community_profile_owner", columnNames = {"owner_id"})
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CommunityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // 프로필 id

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user; // 유저정보

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeltRank beltRank;  // 벨트 등급

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeltStripe beltStripe; // 그랄 등급(1~4)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;  // 성별

    @Column(nullable = false)
    private Double weightKg;    // 체급 (소수점 1자리)

    @Column(length = 100)
    private String academyName;

    @Column
    private Integer competitionYear; // 대회정보(년도)

    @Column(length = 100)
    private String competitionName; // 대회정보(이름)

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private SubmissionType bestSubmission;  // 특기 서브미션

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private SubmissionType favoriteSubmission;  // 최애 서브미션

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private TechniqueType bestTechnique;   // 특기 기술

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private TechniqueType favoriteTechnique;   // 최애 기술

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private PositionType bestPosition;   // 특기 포지션

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private PositionType favoritePosition;   // 최애 포지션

    @Column
    private Boolean weightHidden;   // 체급 숨김여부

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private OwnerProfile ownerProfile;  // 관장사범 프로필

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void toEntity(CommunityProfileRequest request, User user) {
        this.user = user;
        this.beltRank = request.getBeltRank();
        this.beltStripe = request.getBeltStripe();
        this.gender = request.getGender();
        this.weightKg = request.getWeightKg();
        this.academyName = request.getAcademyName();
        this.competitionYear = request.getCompetitionYear();
        this.competitionName = request.getCompetitionName();
        this.bestSubmission = request.getBestSubmission();
        this.favoriteSubmission = request.getFavoriteSubmission();
        this.bestTechnique = request.getBestTechnique();
        this.favoriteTechnique = request.getFavoriteTechnique();
        this.bestPosition = request.getBestPosition();
        this.favoritePosition = request.getFavoritePosition();
        this.weightHidden = request.getIsWeightHidden();
    }
}
