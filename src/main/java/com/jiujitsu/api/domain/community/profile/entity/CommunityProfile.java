package com.jiujitsu.api.domain.community.profile.entity;

import com.jiujitsu.api.domain.community.profile.dto.CompetitionInfoDto;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Entity
@Table(name = "community_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_community_profile_user", columnNames = {"user_id"}),
        @UniqueConstraint(name = "uk_community_profile_owner", columnNames = {"owner_id"})
})
@Getter
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

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "community_profile_competitions",
            joinColumns = @JoinColumn(name = "community_profile_id")
    )
    private List<CompetitionInfo> competitions = new ArrayList<>(); // 대회 정보

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

    /**
     * 최초 User 생성자
     */
    public CommunityProfile(User user) {
        this.user = user;
    }

    /**
     * 도장명 수정
     */
    public void upsertAcademy(String academyName) {
        this.academyName = academyName;
    }

    /**
     * 벨트/성별/체급 수정
     */
    public void upsertBeltWeight(BeltRank beltRank, BeltStripe beltStripe, Gender gender, Double weightKg, Boolean weightHidden) {
        this.beltRank = beltRank;
        this.beltStripe = beltStripe;
        this.gender = gender;
        this.weightKg = weightKg;
        this.weightHidden = weightHidden;
    }

    /**
     * 포지션 수정
     */
    public void upsertBestPosition(PositionType bestPosition) {
        this.bestPosition = bestPosition;
    }

    public void upsertFavoritePosition(PositionType favoritePosition) {
        this.favoritePosition = favoritePosition;
    }

    /**
     * 서브미션 수정
     */
    public void upsertBestSubmission(SubmissionType bestSubmission) {
        this.bestSubmission = bestSubmission;
    }

    public void upsertFavoriteSubmission(SubmissionType favoriteSubmission) {
        this.favoriteSubmission = favoriteSubmission;
    }

    /**
     * 기술 수정
     */
    public void upsertBestTechnique(TechniqueType bestTechnique) {
        this.bestTechnique = bestTechnique;

    }
    public void upsertFavoriteTechnique(TechniqueType favoriteTechnique) {
        this.favoriteTechnique = favoriteTechnique;
    }

    /**
     * 대회정보 수정
     */
    public void upsertCompetitions(List<CompetitionInfoDto> competitionInfos) {
        this.competitions = competitionInfos.isEmpty()
                ? null
                : competitionInfos.stream().map(CompetitionInfoDto::toEntity).collect(Collectors.toList());
    }

    /**
     * 관장 프로필 수정
     */
    public void upsertOwnerInfo(String teachingPhilosophy, LocalDate teachingStartDate, String teachingDetail) {
        this.ownerProfile.update(teachingPhilosophy, teachingStartDate, teachingDetail);
    }

    /**
     * 관장 프로필 등록
     */
    public void insertOwnerProfile(OwnerProfile ownerProfile) {
        this.ownerProfile = ownerProfile;
    }
}
