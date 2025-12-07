package com.jiujitsu.api.domain.community.entity;

import com.jiujitsu.api.domain.community.dto.*;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private BeltRank beltRank;  // 벨트 등급

    @Enumerated(EnumType.STRING)
    private BeltStripe beltStripe; // 그랄 등급(무그랄~4)

    @Enumerated(EnumType.STRING)
    private Gender gender;  // 성별

    private Double weightKg;    // 체급 (소수점 1자리)

    @Column(length = 100)
    private String academyName; // 도장명

    @ElementCollection
    @CollectionTable(
            name = "community_profile_competitions",
            joinColumns = @JoinColumn(name = "community_profile_id")
    )
    private List<CompetitionInfo> competitions = new ArrayList<>(); //대회 정보

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

    public void upsert(CommunityProfileRequest request, User user) {
        this.user = user;
        this.beltRank = request.getBeltRank();
        this.beltStripe = request.getBeltStripe();
        this.gender = request.getGender();
        this.weightKg = request.getWeightKg();
        this.academyName = request.getAcademyName();
        this.bestSubmission = request.getBestSubmission();
        this.favoriteSubmission = request.getFavoriteSubmission();
        this.bestTechnique = request.getBestTechnique();
        this.favoriteTechnique = request.getFavoriteTechnique();
        this.bestPosition = request.getBestPosition();
        this.favoritePosition = request.getFavoritePosition();
        this.weightHidden = request.getIsWeightHidden();
        this.competitions = request.getCompetitionInfoList().stream().map(CompetitionInfoDto::toEntity).collect(Collectors.toList());
        if (Objects.equals(user.getRole(), UserRole.OWNER)) {
            this.ownerProfile.update(request);
        }
    }

    public void insertOwnerProfile(OwnerProfile ownerProfile) {
        this.ownerProfile = ownerProfile;
    }

    public void upsertAcademyInfo(AcademyUpdateRequest request, User user) {
        this.user = user;
        this.academyName = StringUtils.trimToEmpty(request.getAcademyName());
    }

    public void upsertLevelInfo(LevelUpdateRequest request, User user) {
        this.user = user;
        this.beltRank = request.getBeltRank();
        this.beltStripe = request.getBeltStripe();
        this.gender = request.getGender();
        this.weightKg = request.getWeightKg();
        this.weightHidden = request.getIsWeightHidden();
    }

    public void upsertTechniqueInfo(TechniqueUpdateRequest request, User user) {
        this.user = user;

        if (!Objects.isNull(request.getSubmission())) {
            if (Objects.equals(request.getPreferenceType(), TechniqueUpdateRequest.PreferenceType.BEST)) {
                this.bestSubmission = request.getSubmission();
            } else {
                this.favoriteSubmission = request.getSubmission();
            }
        }

        if (!Objects.isNull(request.getTechniqueType())) {
            if (Objects.equals(request.getPreferenceType(), TechniqueUpdateRequest.PreferenceType.BEST)) {
                this.bestTechnique = request.getTechniqueType();
            } else {
                this.favoriteTechnique = request.getTechniqueType();
            }
        }

        if (!Objects.isNull(request.getPosition())) {
            if (Objects.equals(request.getPreferenceType(), TechniqueUpdateRequest.PreferenceType.BEST)) {
                this.bestPosition = request.getPosition();
            } else {
                this.favoritePosition = request.getPosition();
            }
        }
    }
}
