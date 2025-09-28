package com.jiujitsu.api.domain.community.dto;

import com.jiujitsu.api.domain.community.entity.BeltRank;
import com.jiujitsu.api.domain.community.entity.BeltStripe;
import com.jiujitsu.api.domain.community.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Schema(description = "커뮤니티 프로필 수정 요청")
public class CommunityProfileRequest {

    @NotNull
    @Schema(description = "벨트 등급", example = "BLUE")
    private BeltRank beltRank;

    @NotNull
    @Schema(description = "그랄(1~4)", example = "STRIPE_2")
    private BeltStripe beltStripe;

    @NotNull
    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Digits(integer = 5, fraction = 1)
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "300.0", inclusive = true)
    @Schema(description = "체급(kg, 소수점 1자리)", example = "76.5")
    private BigDecimal weightKg;

    @Size(max = 100)
    @Schema(description = "소속 도장명", example = "Gracie Barra Seoul")
    private String academyName;

    @Min(1900)
    @Max(2100)
    @Schema(description = "대회(년도)", example = "2024")
    private Integer competitionYear;

    @Size(max = 100)
    @Schema(description = "대회(이름)", example = "ADCC Korea Trials")
    private String competitionName;

    @Size(max = 100)
    @Schema(description = "좋아하는 기술", example = "Triangle Choke")
    private String favoriteTechnique;

    @Size(max = 100)
    @Schema(description = "자신있는 기술", example = "Armbar")
    private String bestTechnique;
}
