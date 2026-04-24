package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "사용자 프로필 조회 응답")
public record UserProfileResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "닉네임", example = "홍길동") String nickname,
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg") String profileImageUrl,
        @Schema(description = "SNS 제공자", example = "KAKAO") SnsProvider snsProvider,
        @Schema(description = "관장/사범 신청 여부", example = "true") Boolean ownerRequested,
        @Schema(description = "사용자 역할", example = "USER") UserRole role,
        @Schema(description = "사용자 상태", example = "ACTIVE") UserStatus status
) {
}
