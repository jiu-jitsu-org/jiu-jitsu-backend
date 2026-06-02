package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보")
public record UserInfo(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "닉네임", example = "홍길동") String nickname,
        @Schema(description = "프로필 이미지") ImageInfo profileImage,
        @Schema(description = "SNS 제공자", example = "KAKAO") SnsProvider snsProvider,
        @Schema(description = "탈퇴 회원 여부 (30일 이내 재로그인으로 복구된 경우 true)", example = "true") boolean deactivatedWithinGrace
) {
}
