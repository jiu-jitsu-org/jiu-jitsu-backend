package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.SnsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "인증 응답")
public class AuthResponse {

    @Schema(description = "신규회원 여부")
    private Boolean isNewUser;

    @Schema(description = "신규회원 임시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String tempToken;

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "사용자 정보")
    private UserInfo userInfo;

    public AuthResponse(String accessToken, String refreshToken, UserInfo userInfo) {
        this.isNewUser = false;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userInfo = userInfo;
    }

    public AuthResponse(String tempToken) {
        this.isNewUser = true;
        this.tempToken = tempToken;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "사용자 정보")
    public static class UserInfo {
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "이메일", example = "user@example.com")
        private String email;

        @Schema(description = "닉네임", example = "홍길동")
        private String nickname;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private String profileImageUrl;

        @Schema(description = "SNS 제공자", example = "KAKAO")
        private SnsProvider snsProvider;

        @Schema(description = "탈퇴 회원 여부 (30일 이내 재로그인으로 복구된 경우 true)", example = "true")
        private boolean deactivatedWithinGrace;
    }
}
