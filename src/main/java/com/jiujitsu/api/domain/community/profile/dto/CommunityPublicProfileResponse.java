package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.*;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "다른 회원 공개 프로필 응답 (체급·성별·특기·지도 정보 제외)")
public record CommunityPublicProfileResponse(
        @Schema(description = "회원 ID", example = "1") Long userId,
        @Schema(description = "닉네임", example = "홍길동") String nickname,
        @Schema(description = "프로필 이미지") ImageInfo profileImage,
        @Schema(description = "소속 도장명", example = "Gracie Barra Seoul") String academyName,
        @Schema(description = "벨트 등급", example = "BLUE") BeltRank beltRank,
        @Schema(description = "그랄(1~4)", example = "STRIPE_2") BeltStripe beltStripe,
        @Schema(description = "관장/사범 인증 여부") Boolean isOwner,
        @Schema(description = "최애 포지션", example = "GUARD") PositionType favoritePosition,
        @Schema(description = "최애 기술", example = "GUARD_PASSES") TechniqueType favoriteTechnique,
        @Schema(description = "최애 서브미션", example = "ARM_LOCKS") SubmissionType favoriteSubmission,
        @Schema(description = "대회 정보 (대회명이 있는 기록만)") List<CompetitionInfoDto> competitions
) {
}
