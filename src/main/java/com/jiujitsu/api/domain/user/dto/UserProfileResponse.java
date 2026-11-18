package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.AccountType;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 프로필 조회 응답")
public record UserProfileResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "닉네임", example = "홍길동") String nickname,
        @Schema(description = "프로필 이미지") ImageInfo profileImage,
        @Schema(description = "SNS 제공자", example = "KAKAO") SnsProvider snsProvider,
        @Schema(description = "관장/사범 인증 신청 상태") OwnerVerificationInfo ownerVerification,
        @Schema(description = "사용자 역할", example = "USER") UserRole role,
        @Schema(description = "사용자 상태", example = "ACTIVE") UserStatus status,
        @Schema(description = "분석용 회원 ID (GA 회원 ID, 바뀌지 않음)", example = "3f2b8c1e-6a4d-4e2b-9c1a-7d5e0f8b2a91") String analyticsId,
        @Schema(description = "계정 구분 (MEMBER · OPERATOR · INTERNAL)", example = "MEMBER") AccountType accountType
) {
}
