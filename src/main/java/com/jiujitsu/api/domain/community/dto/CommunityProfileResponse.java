package com.jiujitsu.api.domain.community.dto;

import com.jiujitsu.api.domain.community.entity.BeltRank;
import com.jiujitsu.api.domain.community.entity.BeltStripe;
import com.jiujitsu.api.domain.community.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Schema(description = "커뮤니티 프로필 응답")
public class CommunityProfileResponse {

    @Schema(description = "벨트 등급", example = "BLUE")
    private BeltRank beltRank;

    @Schema(description = "그랄(1~4)", example = "STRIPE_2")
    private BeltStripe beltStripe;

    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Schema(description = "체급(kg, 소수점 1자리)", example = "76.5")
    private BigDecimal weightKg;

    @Schema(description = "소속 도장명", example = "Gracie Barra Seoul")
    private String academyName;

    @Schema(description = "대회(년도)", example = "2024")
    private Integer competitionYear;

    @Schema(description = "대회(이름)", example = "ADCC Korea Trials")
    private String competitionName;

    @Schema(description = "좋아하는 기술", example = "Triangle Choke")
    private String favoriteTechnique;

    @Schema(description = "자신있는 기술", example = "Armbar")
    private String bestTechnique;

    @Schema(description = "체급 숨기기 여부")
    private Boolean isWeightHidden;

    public CommunityProfileResponse(CommunityProfile profile) {
        this.beltRank = profile.getBeltRank();
        this.beltStripe = profile.getBeltStripe();
        this.gender = profile.getGender();
        this.weightKg = profile.getWeightKg();
        this.academyName = profile.getAcademyName();
        this.competitionYear = profile.getCompetitionYear();
        this.competitionName = profile.getCompetitionName();
        this.favoriteTechnique = profile.getFavoriteTechnique();
        this.bestTechnique = profile.getBestTechnique();
        this.isWeightHidden = profile.getWeightHidden();
    }
}
