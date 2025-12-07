package com.jiujitsu.api.domain.community.dto;

import com.jiujitsu.api.domain.community.entity.*;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커뮤니티 프로필 응답")
public class CommunityProfileResponse {

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "벨트 등급", example = "BLUE")
    private BeltRank beltRank;

    @Schema(description = "그랄(1~4)", example = "STRIPE_2")
    private BeltStripe beltStripe;

    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Schema(description = "체급(kg, 소수점 1자리)", example = "76.5")
    private Double weightKg;

    @Schema(description = "소속 도장명", example = "Gracie Barra Seoul")
    private String academyName;

    @Schema(description = "대회 정보")
    private List<CompetitionInfoDto> competitions;

    @Schema(description = "특기 서브미션", example = "CHOKES")
    private SubmissionType bestSubmission;

    @Schema(description = "최애 서브미션", example = "ARM_LOCKS")
    private SubmissionType favoriteSubmission;

    @Schema(description = "특기 기술", example = "SWEEPS")
    private TechniqueType bestTechnique;

    @Schema(description = "최애 기술", example = "GUARD_PASSES")
    private TechniqueType favoriteTechnique;

    @Schema(description = "특기 포지션", example = "TOP")
    private PositionType bestPosition;

    @Schema(description = "최애 포지션", example = "GUARD")
    private PositionType favoritePosition;

    @Schema(description = "체급 숨기기 여부")
    private Boolean isWeightHidden;

    @Schema(description = "관장/사범 여부")
    private Boolean isOwner;

    @Schema(description = "(관장/사범) 지도 철학")
    private String teachingPhilosophy;

    @Schema(description = "(관장/사범) 경력 시작일")
    private LocalDate teachingStartDate;

    @Schema(description = "(관장/사범) 경력 상세")
    private String teachingDetail;

    public CommunityProfileResponse(CommunityProfile profile) {
        this.nickname = profile.getUser().getNickname();
        this.profileImageUrl = profile.getUser().getProfileImageUrl();
        this.beltRank = profile.getBeltRank();
        this.beltStripe = profile.getBeltStripe();
        this.gender = profile.getGender();
        this.weightKg = profile.getWeightKg();
        this.academyName = profile.getAcademyName();
        this.competitions = profile.getCompetitions().stream().map(CompetitionInfoDto::new).toList();
        this.bestSubmission = profile.getBestSubmission();
        this.favoriteSubmission = profile.getFavoriteSubmission();
        this.bestTechnique = profile.getBestTechnique();
        this.favoriteTechnique = profile.getFavoriteTechnique();
        this.bestPosition = profile.getBestPosition();
        this.favoritePosition = profile.getFavoritePosition();
        this.isWeightHidden = profile.getWeightHidden();
        this.isOwner = Objects.equals(profile.getUser().getRole(), UserRole.OWNER);
        if (isOwner) {
            Optional<OwnerProfile> ownerProfile = Optional.ofNullable(profile.getOwnerProfile());
            ownerProfile.ifPresent(
                    owner -> {
                        this.teachingPhilosophy = owner.getTeachingPhilosophy();
                        this.teachingStartDate = owner.getTeachingStartDate();
                        this.teachingDetail = owner.getTeachingDetail();
                    }
            );
        }
    }
}
