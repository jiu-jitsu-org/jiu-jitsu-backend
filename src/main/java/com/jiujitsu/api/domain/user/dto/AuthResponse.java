package com.jiujitsu.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 응답")
public record AuthResponse(
        @Schema(description = "신규회원 여부") Boolean isNewUser,
        @Schema(description = "신규회원 임시 토큰", example = "eyJhbGciOiJIUzI1NiIs...")  String tempToken,
        @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIs...") String accessToken,
        @Schema(description = "JWT 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIs...") String refreshToken,
        @Schema(description = "사용자 정보") UserInfo userInfo
) {
}
