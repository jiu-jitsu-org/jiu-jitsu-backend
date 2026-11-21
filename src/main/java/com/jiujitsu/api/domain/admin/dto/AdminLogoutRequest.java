package com.jiujitsu.api.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "관리자 로그아웃 요청")
public record AdminLogoutRequest(
        @NotBlank
        @Schema(description = "액세스 토큰") String accessToken,
        @NotBlank
        @Schema(description = "리프레시 토큰") String refreshToken
) {
}
