package com.jiujitsu.api.domain.community.dto;

import com.jiujitsu.api.domain.community.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private Double weightKg;

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

    @Schema(description = "(관장/사범) 지도 철학")
    private String teachingPhilosophy;

    @Schema(description = "(관장/사범) 경력 시작일")
    private LocalDate teachingStartDate;

    @Schema(description = "(관장/사범) 경력 상세")
    private String teachingDetail;
}
