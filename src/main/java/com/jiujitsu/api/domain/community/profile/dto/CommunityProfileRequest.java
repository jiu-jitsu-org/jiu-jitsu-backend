package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "커뮤니티 프로필 수정 요청")
public record CommunityProfileRequest(
        // 유형별로 따로따로 수정되어서 타입받아서 분리하여 처리
        @Schema(description = "프로필 수정 유형") @NotNull
                ProfileRequestType profileRequestType,
        @Schema(description = "벨트 등급", example = "BLUE")
                BeltRank beltRank,
        @Schema(description = "그랄(1~4)", example = "STRIPE_2")
                BeltStripe beltStripe,
        @Schema(description = "성별", example = "MALE") Gender
                gender,
        @Digits(integer = 5, fraction = 1)
        @DecimalMin(value = "0.0", inclusive = false)
        @DecimalMax(value = "300.0")
        @Schema(description = "체급(kg, 소수점 1자리)", example = "76.5")
                Double weightKg,
        @Size(max = 100)
        @Schema(description = "소속 도장명", example = "Gracie Barra Seoul")
                String academyName,
        @Schema(description = "대회정보")
                List<CompetitionInfoDto> competitionInfoList,
        @Schema(description = "특기 서브미션", example = "CHOKES")
                SubmissionType bestSubmission,
        @Schema(description = "최애 서브미션", example = "ARM_LOCKS")
                SubmissionType favoriteSubmission,
        @Schema(description = "특기 기술", example = "SWEEPS")
                TechniqueType bestTechnique,
        @Schema(description = "최애 기술", example = "GUARD_PASSES")
                TechniqueType favoriteTechnique,
        @Schema(description = "특기 포지션", example = "TOP")
                PositionType bestPosition,
        @Schema(description = "최애 포지션", example = "GUARD")
                PositionType favoritePosition,
        @Schema(description = "체급 숨기기 여부")
                Boolean isWeightHidden,
        @Schema(description = "(관장/사범) 지도 철학")
                String teachingPhilosophy,
        @Schema(description = "(관장/사범) 경력 시작일")
                LocalDate teachingStartDate,
        @Schema(description = "(관장/사범) 경력 상세")
                String teachingDetail
) {
        public List<CompetitionInfoDto> competitionInfoListOrEmpty() {
                return competitionInfoList == null ? List.of() : competitionInfoList;
        }
}


