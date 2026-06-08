package com.jiujitsu.api.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 로그인 응답")
public record AdminLoginResponse(
        @Schema(description = "JWT 액세스 토큰") String accessToken,
        @Schema(description = "JWT 리프레시 토큰") String refreshToken
) {
}
