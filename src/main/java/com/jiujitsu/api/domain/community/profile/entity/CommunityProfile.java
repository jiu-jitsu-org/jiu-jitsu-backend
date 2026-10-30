package com.jiujitsu.api.domain.community.profile.entity;

import com.jiujitsu.api.domain.community.competition.entity.Competition;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
import com.jiujitsu.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
@DynamicUpdate  // 바뀐 컬럼만 UPDATE — 관리자 승인(owner_id)과 본인 프로필 저장이 겹쳐도 서로 덮어쓰지 않게
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
    @OneToMany(mappedBy = "communityProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CompetitionRecord> competitions = new ArrayList<>(); // 대회 정보

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
        this.competitions = new ArrayList<>();  // @Builder.Default는 builder 경로에만 적용된다
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
     * 벨트만 수정 - 성별·체급·체급 숨김은 그대로 둔다
     */
    public void upsertBelt(BeltRank beltRank, BeltStripe beltStripe) {
        this.beltRank = beltRank;
        this.beltStripe = beltStripe;
    }

    /**
     * 포지션 수정
     */
    public void upsertPosition(PositionType bestPosition, PositionType favoritePosition) {
        this.bestPosition = bestPosition;
        this.favoritePosition = favoritePosition;
    }

    /**
     * 서브미션 수정
     */
    public void upsertSubmission(SubmissionType bestSubmission, SubmissionType favoriteSubmission) {
        this.bestSubmission = bestSubmission;
        this.favoriteSubmission = favoriteSubmission;
    }

    /**
     * 기술 수정
     */
    public void upsertTechnique(TechniqueType bestTechnique, TechniqueType favoriteTechnique) {
        this.bestTechnique = bestTechnique;
        this.favoriteTechnique = favoriteTechnique;

    }

    /**
     * 대회정보 수정
     */
    public void upsertCompetitions(List<CompetitionDto> competitionDtos) {
        upsertCompetitions(competitionDtos, Map.of());
    }

    /**
     * 대회정보 수정 - registered 는 competitionId → 등록 대회 (연결한 기록만)
     */
    public void upsertCompetitions(List<CompetitionDto> competitionDtos, Map<Long, Competition> registered) {
        // orphanRemoval 컬렉션은 교체하지 않고 비운 뒤 채운다
        this.competitions.clear();
        competitionDtos.forEach(competition -> this.competitions.add(
                competition.toEntity(this, competition.competitionId() == null ? null : registered.get(competition.competitionId()))));
    }

    /**
     * 관장 프로필 수정
     */
    public void upsertOwnerInfo(String teachingPhilosophy, LocalDate teachingStartDate, String teachingDetail) {
        if (this.ownerProfile == null) {
            throw new IllegalStateException("ownerProfile이 등록되지 않은 사용자입니다.");
        }
        this.ownerProfile.update(teachingPhilosophy, teachingStartDate, teachingDetail);
    }

    /**
     * 관장 프로필 등록
     */
    public void insertOwnerProfile(OwnerProfile ownerProfile) {
        this.ownerProfile = ownerProfile;
    }
}
