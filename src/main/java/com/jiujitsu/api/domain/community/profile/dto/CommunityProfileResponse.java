package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.*;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "커뮤니티 프로필 응답")
public record CommunityProfileResponse(
        @Schema(description = "닉네임", example = "홍길동") String nickname,
        @Schema(description = "프로필 이미지") ImageInfo profileImage,
        @Schema(description = "벨트 등급", example = "BLUE") BeltRank beltRank,
        @Schema(description = "그랄(1~4)", example = "STRIPE_2") BeltStripe beltStripe,
        @Schema(description = "성별", example = "MALE") Gender gender,
        @Schema(description = "체급(kg, 소수점 1자리)", example = "76.5") Double weightKg,
        @Schema(description = "소속 도장명", example = "Gracie Barra Seoul") String academyName,
        @Schema(description = "특기 서브미션", example = "CHOKES") SubmissionType bestSubmission,
        @Schema(description = "최애 서브미션", example = "ARM_LOCKS") SubmissionType favoriteSubmission,
        @Schema(description = "특기 기술", example = "SWEEPS") TechniqueType bestTechnique,
        @Schema(description = "최애 기술", example = "GUARD_PASSES") TechniqueType favoriteTechnique,
        @Schema(description = "특기 포지션", example = "TOP") PositionType bestPosition,
        @Schema(description = "최애 포지션", example = "GUARD") PositionType favoritePosition,
        @Schema(description = "체급 숨기기 여부") Boolean isWeightHidden,
        @Schema(description = "관장/사범 여부") Boolean isOwner,
        @Schema(description = "(관장/사범) 지도 철학") String teachingPhilosophy,
        @Schema(description = "(관장/사범) 경력 시작일") LocalDate teachingStartDate,
        @Schema(description = "(관장/사범) 경력 상세") String teachingDetail,
        @Schema(description = "대회 정보") List<CompetitionInfoDto> competitions
) {
}
